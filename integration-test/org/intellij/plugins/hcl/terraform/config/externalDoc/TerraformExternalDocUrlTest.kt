/*
 * Copyright 2000-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.intellij.plugins.hcl.terraform.config.externalDoc

import com.intellij.openapi.diagnostic.Logger
import com.intellij.testFramework.LightPlatformTestCase
import org.intellij.plugins.hcl.terraform.config.model.TypeModel
import org.intellij.plugins.hcl.terraform.config.model.TypeModelProvider
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.junit.Assert
import org.junit.Test

// Sometimes these tests are really slow, looks like we hit rate limiting on www.terraform.io
class TerraformExternalDocUrlTest : LightPlatformTestCase() {

  private val logger = Logger.getInstance(TerraformExternalDocUrlTest::class.java)

  private val model by lazy { TypeModelProvider.getModel(LightPlatformTestCase.getProject()) }

  private fun httpHead(url: String): Boolean {
    try {
      Jsoup.connect(url).method(Connection.Method.HEAD).execute()
      return true
    }
    catch (e: Exception) {
      logger.warn("${e.message} for $url")
      return false
    }
  }

  @Test
  fun testUrlForBackendDoc() {
    val knownProblems = setOf(
        "inmem"
    )
    val errors = model.backends
        .filterNot { knownProblems.contains(it.type) }
        .toSortedSet(compareBy { it.type })
        .count {
          !httpHead(urlForBackendTypeDoc(it.type))
        }
    Assert.assertEquals(0, errors)
  }

  @Test
  fun testUrlForDataSourceDoc() {
    val knownProblems = setOf(
        "consul_catalog_nodes",
        "consul_catalog_service",
        "consul_catalog_services",
        "flexibleengine_images_image_v2",
        "gitlab_project",
        "gitlab_user",
        "huaweicloud_sfs_file_system_v2",
        "nsxt_certificate",
        "nsxt_logical_tier1_router",
        "nsxt_mac_pool",
        "nsxt_ns_group",
        "oneandone_instance_size",
        "triton_package"
    )
    val errors = model.dataSources
        .filterNot { knownProblems.contains(it.type) }
        .toSortedSet(compareBy { it.type })
        .count { !httpHead(urlForDataSourceTypeDoc(it.provider.type, it.type)) }
    Assert.assertEquals(0, errors)
  }

  @Test
  fun testUrlForFunctionDoc() {
    val doc = Jsoup.connect(FUNCTIONS_URL).get()

    val errors = model.functions
        .toSortedSet(compareBy { it.name })
        .count {
          val fragment = functionFragment(it.name)
          if (doc.getElementsByAttributeValue("name", fragment).isEmpty()) {
            logger.warn("Fragment $fragment not found for ${it.name}() function")
            true
          }
          else {
            false
          }
        }
    Assert.assertEquals(0, errors)
  }

  @Test
  fun testUrlForKeywordDoc() {
    for (block in TypeModel.RootBlocks) {
      Assert.assertTrue(httpHead(urlForKeywordDoc(block.literal)!!))
    }
    Assert.assertTrue(httpHead(urlForKeywordDoc("backend")!!))
    Assert.assertTrue(httpHead(urlForKeywordDoc("provisioner")!!))
  }

  @Test
  fun testUrlForProviderDoc() {
    val errors = model.providers
        .toSortedSet(compareBy { it.type })
        .count {
          !httpHead(urlForProviderTypeDoc(it.type))
        }
    Assert.assertEquals(0, errors)
  }

  @Test
  fun testUrlForProvisionerDoc() {
    val errors = model.provisioners
        .toSortedSet(compareBy { it.type })
        .count {
          !httpHead(urlForProvisionerTypeDoc(it.type))
        }
    Assert.assertEquals(0, errors)
  }

  @Test
  fun testUrlForResourceDoc() {
    val knownProblems = setOf(
        "alicloud_subnet",
        "archive_file",
        "gitlab_project_membership",
        "google_storage_bucket_iam_policy",
        "icinga2_notification",
        "icinga2_user",
        "nsxt_dhcp_server_profile",
        "nsxt_ip_block",
        "nsxt_ip_block_subnet",
        "nsxt_ip_discovery_switching_profile",
        "nsxt_ip_pool",
        "nsxt_lb_client_ssl_profile",
        "nsxt_lb_cookie_persistence_profile",
        "nsxt_lb_fast_tcp_application_profile",
        "nsxt_lb_fast_udp_application_profile",
        "nsxt_lb_http_application_profile",
        "nsxt_lb_http_forwarding_rule",
        "nsxt_lb_http_monitor",
        "nsxt_lb_http_request_rewrite_rule",
        "nsxt_lb_http_response_rewrite_rule",
        "nsxt_lb_http_virtual_server",
        "nsxt_lb_https_monitor",
        "nsxt_lb_icmp_monitor",
        "nsxt_lb_passive_monitor",
        "nsxt_lb_pool",
        "nsxt_lb_server_ssl_profile",
        "nsxt_lb_service",
        "nsxt_lb_source_ip_persistence_profile",
        "nsxt_lb_tcp_monitor",
        "nsxt_lb_tcp_virtual_server",
        "nsxt_lb_udp_monitor",
        "nsxt_lb_udp_virtual_server",
        "nsxt_logical_dhcp_port",
        "nsxt_logical_dhcp_server",
        "nsxt_logical_router_centralized_service_port",
        "nsxt_logical_tier0_router",
        "nsxt_mac_management_switching_profile",
        "nsxt_ns_service_group",
        "nsxt_qos_switching_profile",
        "nsxt_spoofguard_switching_profile",
        "nsxt_switch_security_switching_profile",
        "opc_compute_snapshot",
        "opc_compute_storage_attachment",
        "template_cloudinit_config",
        "template_file",
        "terraform_remote_state"
    )

    val errors = model.resources
        .filterNot { knownProblems.contains(it.type) }
        .toSortedSet(compareBy { it.type })
        .count { !httpHead(urlForResourceTypeDoc(it.provider.type, it.type)) }
    Assert.assertEquals(0, errors)
  }
}
