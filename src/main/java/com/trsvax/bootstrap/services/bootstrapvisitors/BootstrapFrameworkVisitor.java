package com.trsvax.bootstrap.services.bootstrapvisitors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.tapestry5.Asset;
import org.apache.tapestry5.MarkupWriter;
import org.apache.tapestry5.dom.Element;
import org.apache.tapestry5.dom.Visitor;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.AssetSource;
import org.apache.tapestry5.services.Environment;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;
import org.got5.tapestry5.jquery.JQuerySymbolConstants;
import org.slf4j.Logger;

import com.trsvax.bootstrap.FrameworkMixin;
import com.trsvax.bootstrap.FrameworkVisitor;
import com.trsvax.bootstrap.environment.ExcludeEnvironment;

public class BootstrapFrameworkVisitor implements FrameworkVisitor {
	public final static String id = "fw";
	private final Logger logger;	
	private final Environment environment;
	private final AssetSource assetSource;
	private final String jQueryAlias;
	private String ns = "fw";
	private String prefix = ns + ".";
		
	public BootstrapFrameworkVisitor(Logger logger, Environment environment,
			AssetSource assetSource, @Symbol(JQuerySymbolConstants.JQUERY_ALIAS) String alias) {
		this.logger = logger;
		this.environment = environment;
		this.assetSource = assetSource;
		this.jQueryAlias = alias;
	}
	
	public void beginRender(FrameworkMixin component, MarkupWriter writer) {
		//component.getComponentResources().renderInformalParameters(writer);
		//ComponentResources container = component.getComponentResources().getContainerResources();
		String simpleName = component.getComponentResources().getContainer().getClass().getSimpleName();
		//logger.info("Component class {}",simpleName);
		Transform transform = getTransformer(simpleName);
		if ( transform != null ) {
			Element tag = writer.elementNS(ns, prefix + simpleName);
			//component.getComponentResources().renderInformalParameters(writer);
			//logger.info("type {}",container.getInformalParameter("type", String.class));
			for ( Entry<String, String> param : component.getParms().entrySet() ) {
				tag.attribute(param.getKey(),param.getValue());
			}
			transform.beginRender(component, writer);	
		}
	}

	public void afterRender(FrameworkMixin component, MarkupWriter writer) {
		String simpleName = component.getComponentResources().getContainer().getClass().getSimpleName();
		Transform transform = getTransformer(simpleName);
		if ( transform != null ) {
			addHelp(writer.getElement(), component.getComponentResources().getPage().getComponentResources().getMessages());
			writer.end();		
		}
	}

	public Visitor visit() {
		return new Visitor() {
			public void visit(Element element) {
				if ( ns.equals(element.getNamespace())) {				
					String name = element.getName().replace(prefix, "");							
					Transform transform = getTransformer(name);
					//logger.info("visit {} ",name);
					if ( transform != null ) {
						transform.visit(element);	
						element.pop();
					}
					
				}
			}
		};
	}
	
	interface Transform {		
		void beginRender(FrameworkMixin component, MarkupWriter writer);
		void visit(Element element);
	}
	
	class BeanEditForm implements Transform {
		Element root;
		
		public void beginRender(FrameworkMixin component, MarkupWriter writer) {
			
		}

		public void visit(Element element) {
			this.root = element;
			root.visit(beanEditForm());
		}
			
		Visitor beanEditForm() {	
			return new Visitor() {
	
				public void visit(Element element) {
					if (hasClass("t-beaneditor", element)) {
						pop(element);
						element.visit(beanEditForm());
						element.pop();
					}
					if (hasClass("t-beaneditor-row", element)) {
						element.forceAttributes("class", "control-group");
					}
					if ( input(element)) {
						
						String type= element.getAttribute("type");
						String value = element.getAttribute("value") == null ? "" : element.getAttribute("value") ;
						if ( type != null && type.equals("submit") && ! value.equals("Cancel") ) {
							element.addClassName("btn btn-primary");
						} else if ( value.equals("Cancel")) {
							element.addClassName("btn");
						} else {
							element.wrap("div", "class", "control");
						}
					}
					if ( label(element)) {
						element.addClassName("control-label");
					}
					if ( img(element)) {
						element.remove();
					}
				}
			};
		}
	}
	
	class Grid implements Transform {
		Element root;
		List<Element> poplist = new ArrayList<Element>();
		
		public void beginRender(FrameworkMixin component, MarkupWriter writer) {
			
		}

		public void visit(Element element) {
			this.root = element;
			root.visit(grid());
			for ( Element e : poplist ) {
				e.pop();
			}
		}
				
		Visitor grid() {
			return new Visitor() {
				
				public void visit(Element element) {
					String className = root.getAttribute("tabletype");
					if ( table(element)) {
						//element.forceAttributes("class","table");
						if ( className != null ) {
							element.addClassName(className);
						}

					}
					if ( hasName("tbody",element)) {
						String sortable = root.getAttribute("sortable");
						if ( sortable != null && sortable.equals("true")) {
							element.addClassName("sortable");
						}
					}
					if ( hasName("fw.PageLink", element) || hasName("fw.EventLink",element)) {
						new Link().visit(element);
						poplist.add(element);
					}
					if ( img(element) ) {
						String c = element.getAttribute("class");
						if ( c != null && c.equals("t-sort-icon") ) {
							element.elementBefore("span").text("^");
							element.remove();
						}
					}
				}
			};
		}
	}

	class Nav implements Transform {
		Element root;
		List<Element> poplist = new ArrayList<Element>();
		
		public void beginRender(FrameworkMixin component, MarkupWriter writer) {
			importJavaScript("/com/trsvax/bootstrap/pages/twitter/js/bootstrap-button.js");
			importJavaScript("/com/trsvax/bootstrap/pages/twitter/js/bootstrap-dropdown.js");
			scriptOnce(String.format("%s('.makeHash').attr('href','#');",jQueryAlias));
		}

		public void visit(Element element) {
			this.root = element;
			root.visit(nav());
			for ( Element e : poplist ) {
				e.pop();
			}
		}
		
		Visitor nav() {
			return new Visitor() {
				Integer activeLink = 0;
				Integer linkCounter = 1;
				boolean tabbable = false;
				Element ul;

				public void visit(Element element) {
					if (hasName("fw.Nav", element)) {
						String type = element.getAttribute("type");
						if ( type == null ) {
							type = "";
						}
						if ( type.contains("tabbable")) {
							type = type.replace("tabbable", "");
							tabbable = true;
						}
						ul = element.wrap("ul", "class", "nav " + type);
						if ( tabbable ) {
							ul.wrap("div", "class","tabbable");
						}
						
					}
					if ( hasName("fw.ComboButton",element)) {
						element.wrap("li","class","dropdown");
						element.visit(comboButtonNav());
						element.pop();
					}
					if (anchor(element)) {
						if ( isActive(wrapLI(element))) {
							activeLink = linkCounter;
						}
						linkCounter++;
					}
					if ( hasName("fw.Content", element)) {
						element.visit(tabContent(activeLink));
						Element div = element.wrap("div","class","tab-content");
						div.moveAfter(ul);
						element.pop();
						
					}
					if ( hasName("fw.PageLink",element) ) {
						poplist.add(element);
					}
				}
			};
		}
		
		Visitor tabContent(final Integer activeLink) {
			return new Visitor() {
				Integer linkCounter = 1;
				public void visit(Element element) {
					if ( div(element)) {
						element.addClassName("tab-pane");
						if ( linkCounter == activeLink ) {
							element.addClassName("active");
						}
						element.attribute("id", (linkCounter++).toString());
					}
					
				}
			};
		}
		
		Visitor comboButtonNav() {
			return new Visitor() {
				boolean first = true;
				public void visit(Element element) {
					if ( hasName("fw.DropDown",element) ) {
						element.wrap("ul","class","dropdown-menu");
						new Dropdown().visit(element);
						element.pop();
					}
					if ( first && anchor(element)) {
						element.addClassName("dropdown-toggle makeHash");
						element.attribute("data-toggle", "dropdown");
						element.element("b", "class","caret");
						first = false;
					}					
				}
			};
		}
	}
	
	class ButtonGroup implements Transform {
		Element root;
		
		public void beginRender(FrameworkMixin component, MarkupWriter writer) {
			
		}

		public void visit(Element element) {
			this.root = element;
			root.visit(buttonGroup());
		}
		Visitor buttonGroup() {
			return new Visitor() {
				public void visit(Element element) {
					if (hasName("fw.ButtonGroup", element)) {
						element.wrap("div", "class", "btn-group");
					}
					if (anchor(element)) {
						element.addClassName("btn");
					}
				}
			};
		}
	}
	
	class ComboButton implements Transform {
		Element root;
		
		public void beginRender(FrameworkMixin component, MarkupWriter writer) {
			importJavaScript("/com/trsvax/bootstrap/pages/twitter/js/bootstrap-button.js");
			importJavaScript("/com/trsvax/bootstrap/pages/twitter/js/bootstrap-dropdown.js");
		}
		
		public void visit(Element element) {
			this.root = element;
			root.visit(comboButton());
		}
		
		Visitor comboButton() {
			return new Visitor() {
				boolean first = true;
				public void visit(Element element) {
					if ( hasName("fw.ComboButton",element) ) {
						Element div = element.wrap("div", "class","btn-group");
						pop(element);
						div.visit(comboButton());
						
					}
					if ( hasName("fw.DropDown",element) ) {
						Element ul = element.wrap("ul","class","dropdown-menu");
						Element caret = ul.elementBefore("a", "class","btn dropdown-toggle","data-toggle","dropdown","href","#");
						caret.element("span", "class","caret");
						new Dropdown().visit(element);
						element.pop();
					}
					if ( first && anchor(element)) {
						element.addClassName("btn");
						first = false;
					}					
				}
			};
		}
	}
	
	class NavBar implements Transform {
		Element root;
		public void beginRender(FrameworkMixin component, MarkupWriter writer) {
			importJavaScript("/com/trsvax/bootstrap/pages/twitter/js/bootstrap-button.js");
			importJavaScript("/com/trsvax/bootstrap/pages/twitter/js/bootstrap-dropdown.js");
			//importJavaScript("/com/trsvax/bootstrap/pages/twitter/js/bootstrap-scrollspy.js");
		}
		
		public void visit(Element element) {
			this.root = element;
			root.visit(navBar());
		}
		
		Visitor navBar() {
			return new Visitor() {
				
				public void visit(Element element) {
					if ( hasName("fw.NavBar", element)) {
						String projectName = element.getAttribute("ProjectName");
						String type = element.getAttribute("type");
						element.wrap("div", "class","container").wrap("div","class","navbar-inner").wrap("div","class","navbar " + type);
						if ( projectName != null ) {
							element.elementBefore("a", "class","brand","href","#").text(projectName);
						}
					}
					if ( hasName("fw.Nav",element)) {
						new Nav().visit(element);
						element.pop();
					}
					if ( form(element)) {
						element.addClassName("navbar-search");
					}
					if ( input(element)) {
						element.addClassName("search-query");
					}			
				}
			};
		}
	}
	
	class Breadcrumb implements Transform {
		Element root;

		public void beginRender(FrameworkMixin component, MarkupWriter writer) {
			
		}
		
		public void visit(Element element) {
			this.root = element;
			root.wrap("ul", "class","breadcrumb");
			root.visit(breadCrumb());
		}
		
		Visitor breadCrumb() {
			return new Visitor() {
				Element lastLi = null;
				public void visit(Element element) {
					if ( anchor(element) ) {
						Element li = wrapLI(element);
						if ( lastLi != null  ) {
							lastLi.element("span", "class","divider").text("/");
						}
						lastLi = li;
					}				
				}
			};
		}
	}
	
	
	class Dropdown implements Transform {
		Element root;
		public void beginRender(FrameworkMixin component, MarkupWriter writer) {
			importJavaScript("/com/trsvax/bootstrap/pages/twitter/js/bootstrap-button.js");
			importJavaScript("/com/trsvax/bootstrap/pages/twitter/js/bootstrap-dropdown.js");
		}

		public void visit(Element element) {
			this.root = element;
			root.visit(dropdown());
		}
		
		Visitor dropdown() {
			return new Visitor() {
				
				public void visit(Element element) {
					if ( anchor(element)) {
						wrapLI(element);
					}	
				}
			};
		}
	}
	
	class Thumbnails implements Transform {
		Element root;
		public void beginRender(FrameworkMixin component, MarkupWriter writer) {			
		}

		public void visit(Element element) {
			root = element;
			String sortable = element.getAttribute("sortable");
			String id = element.getAttribute("id");
			Element ul = root.wrap("ul","class","thumbnails");
			if ( sortable != null && sortable.equals("true")) {
				ul.addClassName("sortable");
			}
			if ( sortable != null ) {
				ul.attribute("id", id);
			}
			root.visit(thumbnails());			
		}

		Visitor thumbnails() {
			return new Visitor() {
				
				public void visit(Element element) {
					if ( img(element) || hasName("fw.Thumbnail",element)) {
						String span = element.getAttribute("span");
						String id = element.getAttribute("id");
						if ( span != null ) {
							element.wrap("a","href","#","class","thumbnail")
								.wrap("li","class","span" + span,"id",id);
						}
					}
					if ( hasName("fw.Thumbnail",element)) {
						element.pop();
					}
				}
			};
		}	
	}
	
	class Thumbnail implements Transform {

		public void beginRender(FrameworkMixin component, MarkupWriter writer) {			
		}

		public void visit(Element element) {			
		}
		
	}
	
	class Content implements Transform {

		public void visit(Element element) {			
		}

		public void beginRender(FrameworkMixin component, MarkupWriter writer) {			
		}
		
	}
	
	class Link implements Transform {
		Element root;

		public void beginRender(FrameworkMixin component, MarkupWriter writer) {

			
		}

		public void visit(Element element) {
			root = element;	
			root.visit( new Visitor() {				
				public void visit(Element element) {
					if ( anchor(element) ) {
						String type = root.getAttribute("type");
						if ( type != null ) {
							element.addClassName(type);
						}
					}
					
				}
			});
		}		
	}
		
	
	Transform getTransformer(String name) {
		Transform transform = null;
		
		if ( "BeanEditForm".equals(name) ) {
			transform = new BeanEditForm();
		} else if ("Grid".equals(name)) {
			transform = new Grid();
		} else if ("EventLink".equals(name) || "PageLink".equals(name)) {
			transform = new Link();
		} else if ("Nav".equals(name)) {
			transform = new Nav();
		} else if ("ButtonGroup".equals(name)) {
			transform = new ButtonGroup();
		} else if ( "ComboButton".equals(name)) {
			transform = new ComboButton();
		} else if ( "NavBar".equals(name)) {
			transform = new NavBar();
		} else if ( "Breadcrumb".equals(name)) {
			transform = new Breadcrumb();
		} else if ( "DropDown".equals(name)) {
			transform = new Dropdown();
		} else if ("Content".equals(name)) {
			transform = new Content();
		} else if ("Thumbnails".equals(name)) {
			transform = new Thumbnails();
		} else if ("Thumbnail".equals(name)) {
			transform = new Thumbnail();
		} 
		
		return transform;
		
	}
	
	
	
	

	boolean anchor(Element element) {
		return hasName("a", element);
	}
	boolean input(Element element) {
		return hasName("input", element);
	}
	boolean label(Element element) {
		return hasName("label", element);
	}
	boolean img(Element element) {
		return hasName("img", element);
	}
	boolean table(Element element) {
		return hasName("table", element);
	}
	boolean div(Element element) {
		return hasName("div", element);
	}
	boolean form(Element element) {
		return hasName("form", element);
	}

	boolean hasName(String name, Element element) {
		if ( isPopped(element) ) {
			return false;
		}
		if (element.getName().equals(name)) {
			return true;
		}
		return false;
	}

	boolean hasClass(String className, Element element) {
		if ( isPopped(element) ) {
			return false;
		}
		String c = element.getAttribute("class");
		if (c == null || className == null || c.length() == 0
				|| className.length() == 0) {
			return false;
		}
		String[] classes = c.split(" ");
		for (String s : classes) {
			if (className.equals(s)) {
				return true;
			}
		}
		return false;
	}
	
	void pop(Element element) {
		element.attribute("popped", "true");
	}
	
	boolean isPopped(Element element) {
		String pop = element.getAttribute("popped");
		if ( pop == null ) {
			return false;
		}
		if ( pop.equals("true")) {
			return true;
		}
		return false;
	}
	
	boolean isActive(Element element) {	
		String active = element.getAttribute("active");
		if (active != null && active.equals("true")) {
			return true;
		}
		return hasClass("active", element);
	}

	Element wrapLI(Element element) {
		Element li = element.wrap("li");
		if ( isActive(element)) {
			li.addClassName("class", "active");
		}
		return li;
	}
	
	void addHelp(Element element, final Messages messages) {
		element.visit( new Visitor() {
			public void visit(Element element) {
				if (element.getName().equals("input")) {
					String name = element.getAttribute("name");
					if (name != null && messages.contains(name + "-help")) {
						String help = messages.get(name + "-help");
						element.element("div", "class", "help-block").text(help);
					}
				}
			}
		});
	}
	
	void importJavaScript(String lib) {
		JavaScriptSupport javaScriptSupport = environment.peek(JavaScriptSupport.class);
		Asset asset = assetSource.getClasspathAsset(lib);
		javaScriptSupport.importJavaScriptLibrary(asset);
	}
	
	void scriptOnce(String script) {
		ExcludeEnvironment excludeEnvironment = environment.peek(ExcludeEnvironment.class);
		excludeEnvironment.addScriptOnce(script);
	}

}
