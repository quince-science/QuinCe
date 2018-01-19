package uk.ac.exeter.QuinCe.web;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;


import uk.ac.exeter.QuinCe.data.Calculation.CalculationDBFactory;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
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
	 * The available variables
	 */
	private VariableList variables;
	
	/**
	 * A data source
	 */
	private DataSource dataSource;
	
	/**
	 * The dataset being plotted
	 */
	private DataSet dataset;
	
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
	 * @param variables The available variables
	 * @param dataSource A data source
	 * @param dataset The dataset
	 * @param xAxis The x axis variable
	 * @param yAxis The y axis variables
	 * @param mapVariable The map variable
	 */
	public Plot(VariableList variables, DataSource dataSource, DataSet dataset, List<Double> mapBounds, Variable xAxis, List<Variable> yAxis, Variable mapVariable) {
		this.variables = variables;
		this.dataSource = dataSource;
		this.dataset = dataset;
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
		return xAxis.getId();
	}

	/**
	 * @param xAxis the xAxis to set
	 */
	public void setXAxis(int xAxis) {
		this.xAxis = variables.getVariable(xAxis);
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
		return StringUtils.intListToJsonArray(Variable.getIds(yAxis));
	}

	/**
	 * @param yAxis the yAxis to set
	 */
	public void setYAxis(String yAxis) {
		this.yAxis = variables.getVariables(StringUtils.jsonArrayToIntList(yAxis));
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
		return mapVariable.getId();
	}

	/**
	 * @param mapVariable the mapVariable to set
	 */
	public void setMapVariable(int mapVariable) {
		this.mapVariable = variables.getVariable(mapVariable);
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
	
	/**
	 * Set the plot data. Protected method to stop the JSF
	 * front end setting data.
	 * @param data The plot data
	 */
	protected void setPlotData(String data) {
		this.data = data;
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
			result.append(xAxis.getLabel());
			result.append("\",\"");
			result.append("ID");
			result.append("\",\"");
			result.append("Automatic Flag");
			result.append("\",\"");
			result.append("Manual Flag");
			result.append("\",");

			for (int i = 0; i < yAxis.size(); i++) {
				result.append('"');
				result.append(yAxis.get(i).getLabel());
				result.append('"');
				
				if (i < yAxis.size() - 1) {
					result.append(',');
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
			result.append("Automatic Flag");
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
			fields.add("auto_flag");
			fields.add("user_flag");
			
			for (Variable variable : yAxis) {
				fields.add(variable.getFieldName());
			}
			
			break;
		}
		case MODE_MAP: {
			// TODO Remove the magic strings. Make PSF fields in CalculationDB
			fields.add("longitude");
			fields.add("latitude");
			fields.add("date");
			fields.add("id");
			fields.add("auto_flag");
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
			data = CalculationDBFactory.getCalculationDB().getJsonData(dataSource, dataset, getPlotDataFields());
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
            // if (mapUpdateScale) {
                mapScaleLimits = loadMapScaleLimits();
            //}
            mapData = CalculationDBFactory.getCalculationDB().getJsonData(dataSource, dataset, getPlotDataFields(), mapBounds, true);
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
			result = CalculationDBFactory.getCalculationDB().getValueRange(dataSource, dataset, mapVariable.getFieldName());
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
