package uk.ac.exeter.QuinCe.api.test;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Basic POJO for JSON API testing
 * @author zuj007
 *
 */
@XmlRootElement
public class JsonBean {

  private int id;

  private String name;

  public JsonBean() {

  }

  public JsonBean(int id, String name) {
    super();
    this.id = id;
    this.name = name;
  }

  public void setId(int id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

}
