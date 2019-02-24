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
package org.intellij.plugins.hcl.terraform.config.externaldoc

import com.intellij.openapi.diagnostic.Logger
import com.intellij.testFramework.LightPlatformTestCase
import org.intellij.plugins.hcl.terraform.config.model.TypeModel
import org.intellij.plugins.hcl.terraform.config.model.TypeModelProvider
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.junit.Assert
import org.junit.Test

// IMPORTANT NOTE:
// If any of these tests fail the first thing to try is running the gradle task "fetchDocData" then try the test again
//
// Sometimes these tests are very slow, looks like we hit rate limiting on www.terraform.io

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
    val errors = model.backends.keys
        .filterNot { knownProblems.contains(it) }
        .toSortedSet()
        .count {
          !httpHead(urlForBackendTypeDoc(it))
        }
    Assert.assertEquals(0, errors)
  }

  @Test
  fun testUrlForDataSourceDoc() {
    val knownProblems = setOf(
        "consul_catalog_nodes",
        "consul_catalog_service",
        "consul_catalog_services",
        "google_storage_bucket_object",
        "huaweicloud_sfs_file_system_v2",
        "oneandone_instance_size",
        "telefonicaopencloud_csbs_backup_policy_v1",
        "telefonicaopencloud_csbs_backup_v1",
        "telefonicaopencloud_cts_tracker_v1",
        "telefonicaopencloud_dcs_az_v1",
        "telefonicaopencloud_dcs_maintainwindow_v1",
        "telefonicaopencloud_dcs_product_v1",
        "telefonicaopencloud_rds_flavors_v1",
        "telefonicaopencloud_rts_software_config_v1",
        "telefonicaopencloud_rts_stack_resource_v1",
        "telefonicaopencloud_rts_stack_v1",
        "telefonicaopencloud_sfs_file_system_v2",
        "telefonicaopencloud_vbs_backup_policy_v2",
        "telefonicaopencloud_vbs_backup_v2",
        "telefonicaopencloud_vpc_subnet_ids_v1",
        "telefonicaopencloud_vpc_subnet_v1",
        "telefonicaopencloud_vpc_v1",
        "triton_package"
    )
    val errors = model.dataSources
        .filterKeys { !knownProblems.contains(it) }
        .toSortedMap()
        .count { !httpHead(urlForDataSourceTypeDoc(it.value.provider.type, it.key)) }
    Assert.assertEquals(0, errors)
  }

  @Test
  fun testUrlForFunctionDoc() {
    val doc = Jsoup.connect(FUNCTIONS_URL).get()

    val errors = model.functions.keys
        .toSortedSet()
        .count {
          val fragment = functionFragment(it)
          if (doc.getElementsByAttributeValue("name", fragment).isEmpty()) {
            logger.warn("Fragment $fragment not found for $it() function")
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
    val errors = model.providers.keys
        .toSortedSet()
        .count {
          !httpHead(urlForProviderTypeDoc(it))
        }
    Assert.assertEquals(0, errors)
  }

  @Test
  fun testUrlForProvisionerDoc() {
    val errors = model.provisioners.keys
        .toSortedSet()
        .count {
          !httpHead(urlForProvisionerTypeDoc(it))
        }
    Assert.assertEquals(0, errors)
  }

  @Test
  fun testUrlForResourceDoc() {
    val knownProblems = setOf(
        "alicloud_subnet",
        "archive_file",
        "bigip_sys_bigiplicense",
        "bigip_sys_dns",
        "digitalocean_resell_floatingip_v2",
        "digitalocean_resell_keypair_v2",
        "digitalocean_resell_license_v2",
        "digitalocean_resell_project_v2",
        "digitalocean_resell_role_v2",
        "digitalocean_resell_subnet_v2",
        "digitalocean_resell_token_v2",
        "digitalocean_resell_user_v2",
        "digitalocean_resell_vrrp_subnet_v2",
        "github_user_invitation_accepter",
        "google_appengine_firewall_rule",
        "google_project_iam_audit_config",
        "google_storage_bucket_iam_policy",
        "grafana_folder",
        "opc_compute_snapshot",
        "opc_compute_storage_attachment",
        "ovh_ip_reverse",
        "ovh_iploadbalancing_refresh",
        "ovh_iploadbalancing_tcp_frontend",
        "telefonicaopencloud_antiddos_v1",
        "telefonicaopencloud_compute_bms_server_v2",
        "telefonicaopencloud_csbs_backup_policy_v1",
        "telefonicaopencloud_csbs_backup_v1",
        "telefonicaopencloud_cts_tracker_v1",
        "telefonicaopencloud_dcs_instance_v1",
        "telefonicaopencloud_dms_group_v1",
        "telefonicaopencloud_dms_queue_v1",
        "telefonicaopencloud_fw_firewall_group_v2",
        "telefonicaopencloud_fw_policy_v2",
        "telefonicaopencloud_fw_rule_v2",
        "telefonicaopencloud_maas_task_v1",
        "telefonicaopencloud_mrs_cluster_v1",
        "telefonicaopencloud_mrs_job_v1",
        "telefonicaopencloud_rds_instance_v1",
        "telefonicaopencloud_rts_software_config_v1",
        "telefonicaopencloud_rts_stack_v1",
        "telefonicaopencloud_sfs_file_system_v2",
        "telefonicaopencloud_vbs_backup_policy_v2",
        "telefonicaopencloud_vbs_backup_v2",
        "telefonicaopencloud_vpc_peering_connection_accepter_v2",
        "telefonicaopencloud_vpc_peering_connection_v2",
        "telefonicaopencloud_vpc_subnet_v1",
        "telefonicaopencloud_vpc_v1",
        "template_cloudinit_config",
        "template_file",
        "tencentcloud_lb",
        "terraform_remote_state",
        "vault_identity_group",
        "vault_identity_group_alias"
    )

    val errors = model.resources
        .filterKeys { !knownProblems.contains(it) }
        .toSortedMap()
        .count { !httpHead(urlForResourceTypeDoc(it.value.provider.type, it.key)) }
    Assert.assertEquals(0, errors)
  }
}
