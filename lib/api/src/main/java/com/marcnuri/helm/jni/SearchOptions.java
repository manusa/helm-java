package com.marcnuri.helm.jni;

import com.sun.jna.Structure;

@Structure.FieldOrder({"repositoryConfig", "keyword", "regexp", "devel", "version"})
public class SearchOptions extends Structure {

  public String repositoryConfig;
  public String keyword;
  public int regexp;
  public int devel;
  public String version;

  public SearchOptions(String repositoryConfig, String keyword, int regexp, int devel, String version) {
    this.repositoryConfig = repositoryConfig;
    this.keyword = keyword;
    this.regexp = regexp;
    this.devel = devel;
    this.version = version;
  }
}
