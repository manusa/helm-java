package com.marcnuri.helm.jni;

import com.sun.jna.Structure;

@Structure.FieldOrder({
        "releaseName",
        "revision",
        "namespace",
        "kubeConfig",
        "kubeConfigContents"
})
public class GetManifestOptions extends Structure {
    public String releaseName;
    public int revision;
    public String namespace;
    public String kubeConfig;
    public String kubeConfigContents;

    public GetManifestOptions(String releaseName, int revision, String namespace,
                              String kubeConfig, String kubeConfigContents) {
        this.releaseName = releaseName;
        this.revision = revision;
        this.namespace = namespace;
        this.kubeConfig = kubeConfig;
        this.kubeConfigContents = kubeConfigContents;
    }
}
