package io.github.architectplatform.plugins.kubernetes

data class KubernetesContext(
  val namespace: String = "default",
  val context: String = "",
  val manifests: String = "k8s/",
  val kubeconfig: String = "",
  val enabled: Boolean = true,
)
