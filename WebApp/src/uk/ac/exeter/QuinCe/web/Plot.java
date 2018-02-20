package uk.ac.exeter.QuinCe.web;

import java.util.ArrayList;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Calculation.CalculationDBFactory;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Information about a plot on the Plot Page
 *
 */
public class Plot {

	/**
	 * Mode indicator for plot
	 */
	public static final String MODE_PLOT = "plot";
	
	/**
	 * Mode indicator for map
	 */
	public static final String MODE_MAP = "map";

	/**
	 * The bean to which this plot belongs
	 */
	private PlotPageBean parentBean;
	
	/**
	 * The current mode
	 */
	private String mode = MODE_PLOT;
	
	/**
	 * The variable to use on the X Axis in Plot 1.
	 */
	private Variable xAxis;

	/**
	 * The variables to use on the Y Axis in Plot 1.
	 */
	private List<Variable> yAxis;
		
	/**
	 * The plot data
	 */
	private String data;
	
	/**
	 * The variable to show on the map
	 */
	private Variable mapVariable;

	/**
	 * Flag to indicate that the map scale should be updated
	 */
	private boolean mapUpdateScale = false;

	/**
	 * The map data
	 */
	private String mapData;

	/**
	 * The bounds of the map display.
	 * This is a list of [minx, miny, maxx, maxy]
	 */
	private List<Double> mapBounds = null;
	
	/**
	 * The scale limits for the left map
	 */
	private List<Double> mapScaleLimits = null;

	/**
	 * Basic constructor
	 * @param parentBean The bean to which this plot belongs
	 * @param mapBounds The bounds of the map display
	 * @param xAxis The x axis variable
	 * @param yAxis The y axis variables
	 * @param mapVariable The map variable
	 */
	public Plot(PlotPageBean parentBean, List<Double> mapBounds, Variable xAxis, List<Variable> yAxis, Variable mapVariable) {
		this.parentBean = parentBean;
		this.mapBounds = mapBounds;
		this.xAxis = xAxis;
		this.yAxis = yAxis;
		this.mapVariable = mapVariable;
		
		mapScaleLimits = new ArrayList<Double>(4);
		mapScaleLimits.add(0.0);
		mapScaleLimits.add(0.0);
	}

	/**
	 * @return the mode
	 */
	public String getMode() {
		return mode;
	}

	/**
	 * @param mode the mode to set
	 */
	public void setMode(String mode) {
		this.mode = mode;
	}

	/**
	 * @return the xAxis
	 */
	public int getXAxis() {
		int result = -1;
		if (null != xAxis) {
			result = xAxis.getId();
		}

		return result;
	}

	/**
	 * @param xAxis the xAxis to set
	 */
	public void setXAxis(int xAxis) {
		// A -1 value means no change is required
		if (-1 != xAxis) {
			this.xAxis = parentBean.getVariablesList().getVariable(xAxis);
		}
	}

	/**
	 * @param xAxis the xAxis to set
	 */
	public void setXAxis(Variable xAxis) {
		this.xAxis = xAxis;
	}

	/**
	 * @return the yAxis
	 */
	public String getYAxis() {
		
		List<Integer> ids;
		
		if (null == yAxis) {
			ids = new ArrayList<Integer>(1);
			ids.add(-1);
		} else {
			ids = Variable.getIds(yAxis);
		}
		
		return StringUtils.intListToJsonArray(ids);
	}
	
	/**
	 * Get the selected Y Axis variables
	 * @return The selected Y axis variables
	 */
	public List<Variable> getYAxisVariables() {
		return yAxis;
	}

	/**
	 * @param yAxis the yAxis to set
	 */
	public void setYAxis(String yAxis) {
		List<Integer> axisVariables = StringUtils.jsonArrayToIntList(yAxis);
		if (axisVariables.size() > 0) {
			this.yAxis = parentBean.getVariablesList().getVariables(axisVariables);
		}
	}
	
	/**
	 * 
	 * @param yAxis The yAxis to set
	 */
	public void setYAxis(List<Variable> yAxis) {
		this.yAxis = yAxis;
	}
	
	/**
	 * @return the mapVariable
	 */
	public int getMapVariable() {
		int result = -1;
		
		if (null != mapVariable) {
			result = mapVariable.getId();
		}
		
		return result;
	}

	/**
	 * @param mapVariable the mapVariable to set
	 */
	public void setMapVariable(int mapVariable) {
		this.mapVariable = parentBean.getVariablesList().getVariable(mapVariable);
	}

	/**
	 * @param mapVariable the mapVariable to set
	 */
	public void setMapVariable(Variable mapVariable) {
		this.mapVariable = mapVariable;
	}
	
	/**
	 * Get the plot data
	 * @return The plot data
	 */
	public String getData() {
		return data;
	}
	
	// TODO This is horrible. Come up with an alternative.
	//      But we need a way to stop the data coming from the front end
	
	/**
	 * Dummy method for JSF - does nothing
	 * @param data The data from JSF - ignored
	 */
	public void setData(String data) {
		// Do nothing
	}
	
	// TODO This is horrible. Come up with an alternative.
	//      But we need a way to stop the labels coming from the front end

	/**
	 * Get the data labels for the plot
	 * @return The data labels
	 */
	public String getLabels() {
		StringBuilder result = new StringBuilder();
		
		result.append('[');

		
		switch(mode) {
		case MODE_PLOT: {
			
			// TODO Remove the magic strings. Make PSF fields in CalculationDB
			result.append('"');
			if (null != xAxis) {
				result.append(xAxis.getLabel());
			}
			result.append("\",\"ID\",\"Manual Flag\"");
			
			if (null != yAxis) {
				result.append(',');

				for (int i = 0; i < yAxis.size(); i++) {
					result.append('"');
					result.append(yAxis.get(i).getLabel());
					result.append('"');
					
					if (i < yAxis.size() - 1) {
						result.append(',');
					}
				}
			}
			
			break;
		}
		case MODE_MAP: {
			// TODO Remove the magic strings. Make PSF fields in CalculationDB
			result.append('"');
			result.append("Longitude");
			result.append("\",\"");
			result.append("Longitude");
			result.append("\",\"");
			result.append("Date/Time");
			result.append("\",\"");
			result.append("ID");
			result.append("\",\"");
			result.append("Manual Flag");
			result.append("\",\"");
			result.append(mapVariable.getLabel());
			result.append('"');
		}
		}
		
		result.append(']');
		
		return result.toString();
	}
	
	/**
	 * Dummy method for the front end
	 * @param labels Ignored
	 */
	public void setLabels(String labels) {
		// Do nothing
	}
	
	/**
	 * Get the fields required for the plot in its current configuration
	 * @return The fields
	 */
	private List<String> getPlotDataFields() {
		
		List<String> fields = new ArrayList<String>();
		
		switch(mode) {
		case MODE_PLOT: {
			// TODO Remove the magic strings. Make PSF fields in CalculationDB
			fields.add(xAxis.getFieldName());
			fields.add("id");
			fields.add("user_flag");
			
			if (null != yAxis) {
				for (Variable variable : yAxis) {
					fields.add(variable.getFieldName());
				}
			}
			
			break;
		}
		case MODE_MAP: {
			// TODO Remove the magic strings. Make PSF fields in CalculationDB
			fields.add("longitude");
			fields.add("latitude");
			fields.add("date");
			fields.add("id");
			fields.add("user_flag");
			fields.add(mapVariable.getFieldName());
		}
		}

		return fields;
	}
	
	/**
	 * Update the plot data
   	 */
	public void updatePlot() {
		try {
			data = parentBean.getData(getPlotDataFields());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return the updateScale
	 */
	public boolean getMapUpdateScale() {
		return mapUpdateScale;
	}

	/**
	 * @param mapUpdateScale the updateScale to set
	 */
	public void setMapUpdateScale(boolean mapUpdateScale) {
		this.mapUpdateScale = mapUpdateScale;
	}

	/**
	 * @return the mapData
	 */
	public String getMapData() {
		return mapData;
	}

	/**
	 * @param mapData the mapData to set; ignored
	 */
	public void setMapData(String mapData) {
		// Do nothing
	}

	/**
	 * @return the mapBounds
	 */
	public String getMapBounds() {
		return '[' + StringUtils.listToDelimited(mapBounds, ",") + ']';
	}

	/**
	 * @param mapBounds the mapBounds to set
	 */
	public void setMapBounds(String mapBounds) {
		this.mapBounds = StringUtils.delimitedToDoubleList(mapBounds.substring(1, mapBounds.length() - 1), ",");
	}

	/**
	 * @return the mapBounds
	 */
	public String getMapScaleLimits() {
		return '[' + StringUtils.listToDelimited(mapScaleLimits, ",") + ']';
	}
	
	/**
	 * @param mapScaleLimits the scaleLimits to set
	 */
	public void setMapScaleLimits(String mapScaleLimits) {
		// Do nothing
	}
	
	public void generateMapData() {
		try {
            // if (mapUpdateScale) { // This doesn't work well. Since the performance hit is small, leave out the check for now.
                mapScaleLimits = loadMapScaleLimits();
            //}
            mapData = CalculationDBFactory.getCalculationDB().getJsonData(parentBean.getDataSource(), parentBean.getDataset(), getPlotDataFields(), null, mapBounds, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the value range for a column being shown on a map
	 * @return The value range
	 */
	private List<Double> loadMapScaleLimits() {
		List<Double> result = null;
		
		try {
			// TODO This is specific to the Manual QC. Move it out to a method in the parent bean
			result = CalculationDBFactory.getCalculationDB().getValueRange(parentBean.getDataSource(), parentBean.getDataset(), mapVariable.getFieldName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (null == result) {
			result = new ArrayList<Double>(2);
			result.add(0.0);
			result.add(0.0);
		}
		
		return result;
	}
}
