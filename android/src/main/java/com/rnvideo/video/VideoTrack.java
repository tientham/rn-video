package com.rnvideo.video;

class VideoTrack {
  Integer width = 0;
  Integer height = 0;
  Integer bitrate = 0;
  String codecs = "";
  Integer id = -1;
  String trackId = "";
  Boolean isSelected = false;

  public void setBitrate(Integer bitrate) {
    this.bitrate = bitrate;
  }

  public void setCodecs(String codecs) {
    this.codecs = codecs;
  }

  public void setHeight(Integer height) {
    this.height = height;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public void setSelected(Boolean selected) {
    isSelected = selected;
  }

  public void setTrackId(String trackId) {
    this.trackId = trackId;
  }

  public void setWidth(Integer width) {
    this.width = width;
  }

  public Boolean getSelected() {
    return isSelected;
  }
}
