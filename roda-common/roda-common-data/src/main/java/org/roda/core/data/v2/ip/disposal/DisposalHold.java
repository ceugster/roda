package org.roda.core.data.v2.ip.disposal;

import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.IsModelObject;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Tiago Fraga <tfraga@keep.pt>
 */

@javax.xml.bind.annotation.XmlRootElement(name = RodaConstants.RODA_OBJECT_DISPOSAL_HOLD)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DisposalHold implements IsModelObject {

  private static final long serialVersionUID = 8291490773422089586L;

  private String id;
  private Date creationTimestamp;
  private String title;
  private String description;
  private String mandate;
  private String scopeNotes;
  private Date destructionTimestamp;

  public DisposalHold(String disposalHoldId, String title, String description, String mandate, String scopeNotes) {
  }

  @JsonIgnore
  @Override
  public int getClassVersion() {
    return 1;
  }

  @Override
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Date getCreationTimestamp() {
    return creationTimestamp;
  }

  public void setCreationTimestamp(Date creationTimestamp) {
    this.creationTimestamp = creationTimestamp;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getMandate() {
    return mandate;
  }

  public void setMandate(String mandate) {
    this.mandate = mandate;
  }

  public String getScopeNotes() {
    return scopeNotes;
  }

  public void setScopeNotes(String scopeNotes) {
    this.scopeNotes = scopeNotes;
  }

  public Date getDestructionTimestamp() {
    return destructionTimestamp;
  }

  public void setDestructionTimestamp(Date destructionTimestamp) {
    this.destructionTimestamp = destructionTimestamp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    DisposalHold that = (DisposalHold) o;
    return id.equals(that.id) && creationTimestamp.equals(that.creationTimestamp) && title.equals(that.title)
      && description.equals(that.description) && mandate.equals(that.mandate) && scopeNotes.equals(that.scopeNotes)
      && destructionTimestamp.equals(that.destructionTimestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, creationTimestamp, title, description, mandate, scopeNotes, destructionTimestamp);
  }

  @Override
  public String toString() {
    return "DisposalHold{" + "id='" + id + '\'' + ", creationTimestamp=" + creationTimestamp + ", title='" + title
      + '\'' + ", description='" + description + '\'' + ", mandate='" + mandate + '\'' + ", scopeNotes='" + scopeNotes
      + '\'' + ", destructionTimestamp=" + destructionTimestamp + '}';
  }
}