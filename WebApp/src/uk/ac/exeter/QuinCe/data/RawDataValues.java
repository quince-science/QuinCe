package uk.ac.exeter.QuinCe.data;

import java.util.Calendar;

public class RawDataValues {

	private long dataFileId;
	
	private int row;
	
	private Calendar time;
	
	private long runTypeId;
	
	private int co2Type;
	
	private double longitude;
	
	private double latitude;
	
	private double intakeTemp1; 
	
	private double intakeTemp2; 

	private double intakeTemp3; 
	
	private double salinity1; 
	
	private double salinity2; 
	
	private double salinity3; 
	
	private double eqt1; 
	
	private double eqt2; 
	
	private double eqt3; 
	
	private double eqp1; 
	
	private double eqp2; 
	
	private double eqp3;
	
	private double airFlow1;
	
	private double airFlow2;
	
	private double airFlow3;
	
	private double waterFlow1;
	
	private double waterFlow2;
	
	private double waterFlow3;
	
	private double moisture;
	
	private double atmosphericPressure;
	
	private double co2;
	
	public RawDataValues(long dataFileId, int row) {
		this.dataFileId = dataFileId;
		this.row = row;
	}
	
	public long getRunTypeId() {
		return runTypeId;
	}
	
	public void setRunTypeId(long runTypeId) {
		this.runTypeId = runTypeId;
	}

	public int getCo2Type() {
		return co2Type;
	}

	public void setCo2Type(int co2Type) {
		this.co2Type = co2Type;
	}

	public double getIntakeTemp1() {
		return intakeTemp1;
	}

	public void setIntakeTemp1(double intakeTemp1) {
		this.intakeTemp1 = intakeTemp1;
	}

	public double getIntakeTemp2() {
		return intakeTemp2;
	}

	public void setIntakeTemp2(double intakeTemp2) {
		this.intakeTemp2 = intakeTemp2;
	}

	public double getIntakeTemp3() {
		return intakeTemp3;
	}

	public void setIntakeTemp3(double intakeTemp3) {
		this.intakeTemp3 = intakeTemp3;
	}

	public double getSalinity1() {
		return salinity1;
	}

	public void setSalinity1(double salinity1) {
		this.salinity1 = salinity1;
	}

	public double getSalinity2() {
		return salinity2;
	}

	public void setSalinity2(double salinity2) {
		this.salinity2 = salinity2;
	}

	public double getSalinity3() {
		return salinity3;
	}

	public void setSalinity3(double salinity3) {
		this.salinity3 = salinity3;
	}

	public double getEqt1() {
		return eqt1;
	}

	public void setEqt1(double eqt1) {
		this.eqt1 = eqt1;
	}

	public double getEqt2() {
		return eqt2;
	}

	public void setEqt2(double eqt2) {
		this.eqt2 = eqt2;
	}

	public double getEqt3() {
		return eqt3;
	}

	public void setEqt3(double eqt3) {
		this.eqt3 = eqt3;
	}

	public double getEqp1() {
		return eqp1;
	}

	public void setEqp1(double eqp1) {
		this.eqp1 = eqp1;
	}

	public double getEqp2() {
		return eqp2;
	}

	public void setEqp2(double eqp2) {
		this.eqp2 = eqp2;
	}

	public double getEqp3() {
		return eqp3;
	}

	public void setEqp3(double eqp3) {
		this.eqp3 = eqp3;
	}

	public double getAirFlow1() {
		return airFlow1;
	}

	public void setAirFlow1(double airFlow1) {
		this.airFlow1 = airFlow1;
	}

	public double getAirFlow2() {
		return airFlow2;
	}

	public void setAirFlow2(double airFlow2) {
		this.airFlow2 = airFlow2;
	}

	public double getAirFlow3() {
		return airFlow3;
	}

	public void setAirFlow3(double airFlow3) {
		this.airFlow3 = airFlow3;
	}

	public double getWaterFlow1() {
		return waterFlow1;
	}

	public void setWaterFlow1(double waterFlow1) {
		this.waterFlow1 = waterFlow1;
	}

	public double getWaterFlow2() {
		return waterFlow2;
	}

	public void setWaterFlow2(double waterFlow2) {
		this.waterFlow2 = waterFlow2;
	}

	public double getWaterFlow3() {
		return waterFlow3;
	}

	public void setWaterFlow3(double waterFlow3) {
		this.waterFlow3 = waterFlow3;
	}

	public double getMoisture() {
		return moisture;
	}

	public void setMoisture(double moisture) {
		this.moisture = moisture;
	}

	public double getAtmosphericPressure() {
		return atmosphericPressure;
	}

	public void setAtmosphericPressure(double atmosphericPressure) {
		this.atmosphericPressure = atmosphericPressure;
	}
	
	public double getCo2() {
		return co2;
	}
	
	public void setCo2(double co2) {
		this.co2 = co2;
	}

	public long getDataFileId() {
		return dataFileId;
	}

	public int getRow() {
		return row;
	}
	
	public void setTime(Calendar time) {
		this.time = time;
	}
	
	public Calendar getTime() {
		return time;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
}
