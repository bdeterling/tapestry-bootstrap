package com.trsvax.bootstrap.components;

import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.Block;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.annotations.Component;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SetupRender;
import org.apache.tapestry5.annotations.SupportsInformalParameters;
import org.apache.tapestry5.ioc.annotations.Inject;
import java.util.List;

@SupportsInformalParameters
@SuppressWarnings("unused")
public class BeanGrid<T> extends BootstrapComponent {
	@Parameter(autoconnect=true,required=true,allowNull=false)
	@Property
	private List<?> source;
	
	@Parameter
	@Property
	private Object value;
	
	@Parameter(value="resources.id")
	@Property
	private String parameterName;
	
	@Parameter
	@Property
	private Integer index;
	
	@Inject
	@Property
	private ComponentResources resources;
	
	@Property
	private String tableType;
	
	@Component(parameters={"value=value","index=index"})
	private Loop<T> loop;
    
    @Inject
    private Block grid;
    
    /**
     * A Block to render instead of the table (and pager, etc.) when the source is empty. The default is simply the text
     * "There is no data to display". This parameter is used to customize that message, possibly including components to
     * allow the user to create new objects.
     */
    @Parameter(value = "block:empty", defaultPrefix = BindingConstants.LITERAL)
    private Block empty;
	
	@SetupRender
	private void setupRender() {
        if (source.size() > 0)
            value = source.get(0);
		tableType = resources.getInformalParameter("class", String.class);
	}
    
    public Block getBlockForGrid() {
        if (value == null)
            return empty;
        else
            return grid;
    }

}
