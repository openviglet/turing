<?php
/*
 * Copyright (c) 2017 Viglet
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

// get the plugin settings
$dumont4wp_settings = dumont4wp_get_option('plugin_dumont4wp_settings');

// get a a list of all the available content types so we render out some options
$post_types = dumont4wp_get_all_post_types();

// set defaults if not initialized
if ($dumont4wp_settings['dumont4wp_solr_initialized'] != 1) {

	$options['dumont4wp_index_all_sites'] = 0;
	$options['dumont4wp_server']['info']['single'] = array(
		'host' => 'localhost',
		'port' => 2700,
		'path' => '/dumont'
	);
	$options['dumont4wp_server']['info']['master'] = array(
		'host' => 'localhost',
		'port' => 2700,
		'path' => '/dumont'
	);
	$options['dumont4wp_server']['type']['search'] = 'master';
	$options['dumont4wp_server']['type']['update'] = 'master';

	// by default we index pages and posts, and remove them from index if there status changes.
	$options['dumont4wp_content']['index'] = array(
		'page' => '1',
		'post' => '1'
	);
	$options['dumont4wp_content']['delete'] = array(
		'page' => '1',
		'post' => '1'
	);
	$options['dumont4wp_content']['private'] = array(
		'page' => '1',
		'post' => '1'
	);

	$options['dumont4wp_index_pages'] = 1;
	$options['dumont4wp_index_posts'] = 1;
	$options['dumont4wp_delete_page'] = 1;
	$options['dumont4wp_delete_post'] = 1;
	$options['dumont4wp_private_page'] = 1;
	$options['dumont4wp_private_post'] = 1;
	$options['dumont4wp_output_info'] = 1;
	$options['dumont4wp_output_pager'] = 1;
	$options['dumont4wp_output_facets'] = 1;
	$options['dumont4wp_exclude_pages'] = array();
	$options['dumont4wp_exclude_pages'] = '';
	$options['dumont4wp_num_results'] = 5;
	$options['dumont4wp_cat_as_taxo'] = 1;
	$options['dumont4wp_solr_initialized'] = 1;
	$options['dumont4wp_max_display_tags'] = 10;
	$options['dumont4wp_facet_on_categories'] = 1;
	$options['dumont4wp_facet_on_taxonomy'] = 1;
	$options['dumont4wp_facet_on_tags'] = 1;
	$options['dumont4wp_facet_on_author'] = 1;
	$options['dumont4wp_facet_on_type'] = 1;
	$options['dumont4wp_enable_dym'] = 1;
	$options['dumont4wp_index_comments'] = 1;
	$options['dumont4wp_connect_type'] = 'solr';
	$options['dumont4wp_index_custom_fields'] = array();
	$options['dumont4wp_facet_on_custom_fields'] = array();
	$options['dumont4wp_index_custom_fields'] = '';
	$options['dumont4wp_facet_on_custom_fields'] = '';

	// update existing settings from multiple option record to a single array
	// if old options exist, update to new system
	$delete_option_function = 'delete_option';
	if (is_multisite()) {
		$indexall = get_site_option('dumont4wp_index_all_sites');
		$delete_option_function = 'delete_site_option';
	}
	// find each of the old options function
	// update our new array and delete the record.
	foreach ($options as $key => $value) {
		if ($existing = get_option($key)) {
			$options[$key] = $existing;
			$indexall = FALSE;
			// run the appropriate delete options function
			$delete_option_function($key);
		}
	}

	$dumont4wp_settings = $options;
	// save our options array
	dumont4wp_update_option($options);
}

wp_reset_vars(array(
	'action'
));

// save form settings if we get the update action
// we do saving here instead of using options.php because we need to use
// dumont4wp_update_option instead of update option.
// As it stands we have 27 options instead of making 27 insert calls (which is what update_options does)
// Lets create an array of all our options and save it once.
if ($_POST['action'] == 'update') {
	// lets loop through our setting fields $_POST['settings']
	foreach ($dumont4wp_settings as $option => $old_value) {
		$value = $_POST['settings'][$option];

		switch ($option) {
			case 'dumont4wp_solr_initialized':
				$value = trim($old_value);
				break;

			case 'dumont4wp_server':
				// remove empty server entries
				$s_value = &$value['info'];

				foreach ($s_value as $key => $v) {
					// lets rename the array_keys
					if (! $v['host'])
						unset($s_value[$key]);
				}
				break;
		}
		if (! is_array($value))
			$value = trim($value);
		$value = stripslashes_deep($value);
		$dumont4wp_settings[$option] = $value;
	}

	$dumont4wp_settings['dumont4wp_server']['info']['master'] = $dumont4wp_settings['dumont4wp_server']['info']['single'];
	$dumont4wp_settings['dumont4wp_server']['type']['search'] = 'master';
	$dumont4wp_settings['dumont4wp_server']['type']['update'] = 'master';
	// lets save our options array
	dumont4wp_update_option($dumont4wp_settings);

	// we need to make call for the options again
	// as we need them to come out in an a sanitised format
	// otherwise fields that need to run dumont4wp_filter_list2str will come up with nothin
	$dumont4wp_settings = dumont4wp_get_option('plugin_dumont4wp_settings');
?>
	<div id="message" class="updated fade">
		<p>
			<strong><?php _e('Success!', 'dumont4wp') ?></strong>
		</p>
	</div>
	<?php
}

// checks if we need to check the checkbox
function dumont4wp_checkCheckbox($fieldValue, $option = array(), $field = false)
{
	$option_value = (is_array($option) && $field) ? $option[$field] : $option;
	if ($fieldValue == '1' || $option_value == '1') {
		echo 'checked="checked"';
	}
}

function dumont4wp_checkConnectOption($optionType, $connectType)
{
	if ($optionType === $connectType) {
		echo 'checked="checked"';
	}
}

// check for any POST settings
if ($_POST['dumont4wp_ping']) {
	if (dumont4wp_ping_server()) {
	?>
		<div id="message" class="updated fade">
			<p>
				<strong><?php _e('Ping Success!', 'dumont4wp') ?></strong>
			</p>
		</div>
	<?php
	} else {
	?>
		<div id="message" class="updated fade">
			<p>
				<strong><?php _e('Ping Failed!', 'dumont4wp') ?></strong>
			</p>
		</div>
	<?php
	}
} else if ($_POST['dumont4wp_deleteall']) {
	dumont4wp_delete_all();
	?>
	<div id="message" class="updated fade">
		<p>
			<strong><?php _e('All Indexed Pages Deleted!', 'dumont4wp') ?></strong>
		</p>
	</div>
<?php
} else if ($_POST['dumont4wp_optimize']) {
	dumont4wp_optimize();
?>
	<div id="message" class="updated fade">
		<p>
			<strong><?php _e('Index Optimized!', 'dumont4wp') ?></strong>
		</p>
	</div>
<?php
} else if ($_POST['dumont4wp_init_blogs']) {
	dumont4wp_copy_config_to_all_blogs();
?>
	<div id="message" class="updated fade">
		<p>
			<strong><?php _e('Solr for Wordpress Configured for All Blogs!', 'dumont4wp') ?></strong>
		</p>
	</div>

<?php } ?>
<div class="wrap">
	<h2><?php _e('Viglet Dumont For WordPress', 'dumont4wp') ?></h2>

	<form method="post"
		action="options-general.php?page=dumont4wp/viglet-dumont-for-wordpress.php">
		<h3><?php _e('Configure Viglet Dumont', 'dumont4wp') ?></h3>


		<table class="form-table">
			<tr>

				<th scope="row"><label
						for="settings[dumont4wp_server][info][single][host]">
						<?php _e('Host', 'dumont4wp') ?></label></th>
				<td><input type="text"
						name="settings[dumont4wp_server][info][single][host]"
						value="<?php echo $dumont4wp_settings['dumont4wp_server']['info']['single']['host'] ?>" /></td>
			</tr>
			<tr>

				<th scope="row"><label
						for="settings[dumont4wp_server][info][single][port]">
						<?php _e('Port', 'dumont4wp') ?></label></th>
				<td><input type="text"
						name="settings[dumont4wp_server][info][single][port]"
						value="<?php echo $dumont4wp_settings['dumont4wp_server']['info']['single']['port'] ?>" /></td>
			</tr>
			<tr>

				<th scope="row"><label
						for="settings[dumont4wp_server][info][single][path]">
						<?php _e('Path', 'dumont4wp') ?></label></th>
				<td><input type="text"
						name="settings[dumont4wp_server][info][single][path]"
						value="<?php echo $dumont4wp_settings['dumont4wp_server']['info']['single']['path'] ?>" /></td>
			</tr>
			<tr>

				<th scope="row"><label
						for="settings[dumont4wp_server][info][single][siteName]">
						<?php _e('Site Name', 'dumont4wp') ?></label></th>
				<td><input type="text"
						name="settings[dumont4wp_server][info][single][siteName]"
						value="<?php echo $dumont4wp_settings['dumont4wp_server']['info']['single']['siteName'] ?>" /></td>
			</tr>
		</table>

		<!-- Solr Config -->
		<div class="solr_admin clearfix">
			<div class="solr_adminR">
				<div class="solr_adminR2" id="solr_admin_tab3">
					<table>
						<tr>
							<?php
							// we are working with multiserver setup so lets
							// lets provide an extra fields for extra host on the fly by appending an empty array
							// this will always give a count of current servers+1
							$serv_count = count($dumont4wp_settings['dumont4wp_server']['info']);
							$dumont4wp_settings['dumont4wp_server']['info'][$serv_count] = array(
								'host' => '',
								'port' => '',
								'path' => ''
							);
							foreach ($dumont4wp_settings['dumont4wp_server']['info'] as $server_id => $server) {
								if ($server_id == "single")
									continue;
								// lets set serverIDs
								$new_id = (is_numeric($server_id)) ? 'slave_' . $server_id : $server_id;
							?>
								<td><label><?php _e('ServerID', 'dumont4wp') ?>: <strong><?php echo $new_id; ?></strong></label>
									<p>
										Update Server: &nbsp;&nbsp;<input
											name="settings[dumont4wp_server][type][update]" type="radio"
											value="<?php echo $new_id ?>"
											<?php dumont4wp_checkConnectOption($dumont4wp_settings['dumont4wp_server']['type']['update'], $new_id); ?> />
									</p>
									<p>
										Search Server: &nbsp;&nbsp;<input
											name="settings[dumont4wp_server][type][search]" type="radio"
											value="<?php echo $new_id ?>"
											<?php dumont4wp_checkConnectOption($dumont4wp_settings['dumont4wp_server']['type']['search'], $new_id); ?> />
									</p> <label><?php _e('Solr Host', 'dumont4wp') ?></label>
									<p>
										<input type="text"
											name="settings[dumont4wp_server][info][<?php echo $new_id ?>][host]"
											value="<?php echo $server['host'] ?>" />
									</p> <label><?php _e('Solr Port', 'dumont4wp') ?></label>
									<p>
										<input type="text"
											name="settings[dumont4wp_server][info][<?php echo $new_id ?>][port]"
											value="<?php echo $server['port'] ?>" />
									</p> <label><?php _e('Solr Path', 'dumont4wp') ?></label>
									<p>
										<input type="text"
											name="settings[dumont4wp_server][info][<?php echo $new_id ?>][path]"
											value="<?php echo $server['path'] ?>" />
									</p>
								</td>
							<?php
							}
							?>
						</tr>
					</table>
				</div>
			</div>

		</div>
		<hr />
		<h3><?php _e('Indexing Options', 'dumont4wp') ?></h3>
		<table class="form-table">
			<?php
			foreach ($post_types as $post_key => $post_type) {
			?>
				<tr valign="top">
					<th scope="row" style="width: 200px;"><?php _e('Index ' . ucfirst($post_type), 'dumont4wp') ?></th>
					<td style="width: 10px; float: left;"><input type="checkbox"
							name="settings[dumont4wp_content][index][<?php echo $post_type ?>]"
							value="1"
							<?php echo dumont4wp_checkCheckbox(FALSE, $dumont4wp_settings['dumont4wp_content']['index'], $post_type); ?> /></td>

					<th scope="row" style="width: 200px;"><?php _e('Remove ' . ucfirst($post_type) . ' on Delete', 'dumont4wp') ?></th>
					<td style="width: 10px; float: left;"><input type="checkbox"
							name="settings[dumont4wp_content][delete][<?php echo $post_type ?>]"
							value="1"
							<?php echo dumont4wp_checkCheckbox(FALSE, $dumont4wp_settings['dumont4wp_content']['delete'], $post_type); ?> /></td>

					<th scope="row" style="width: 200px;"><?php _e('Remove ' . ucfirst($post_type) . ' on Status Change', 'dumont4wp') ?></th>
					<td style="width: 10px; float: left;"><input type="checkbox"
							name="settings[dumont4wp_content][private][<?php echo $post_type ?>]"
							value="1"
							<?php echo dumont4wp_checkCheckbox(FALSE, $dumont4wp_settings['dumont4wp_content']['private'], $post_type); ?> /></td>
				</tr>
			<?php } ?>

			<tr valign="top">
				<th scope="row" style="width: 200px;"><?php _e('Index Comments', 'dumont4wp') ?></th>
				<td style="width: 10px; float: left;"><input type="checkbox"
						name="settings[dumont4wp_index_comments]" value="1"
						<?php echo dumont4wp_checkCheckbox($dumont4wp_settings['dumont4wp_index_comments']); ?> /></td>
			</tr>

			<?php
			// is this a multisite installation
			if (is_multisite() && is_main_site()) {
			?>

				<tr valign="top">
					<th scope="row" style="width: 200px;"><?php _e('Index all Sites', 'dumont4wp') ?></th>
					<td style="width: 10px; float: left;"><input type="checkbox"
							name="settings[dumont4wp_index_all_sites]" value="1"
							<?php echo dumont4wp_checkCheckbox($dumont4wp_settings['dumont4wp_index_all_sites']); ?> /></td>
				</tr>
			<?php
			}
			?>
			<tr valign="top">
				<th scope="row"><?php _e('Index custom fields (comma separated names list)') ?></th>
				<td><input type="text"
						name="settings[dumont4wp_index_custom_fields]"
						value="<?php print(dumont4wp_filter_list2str($dumont4wp_settings['dumont4wp_index_custom_fields'], 'dumont4wp')); ?>" /></td>
			</tr>
			<tr valign="top">
				<th scope="row"><?php _e('Excludes Posts or Pages (comma separated ids list)') ?></th>
				<td><input type="text" name="settings[dumont4wp_exclude_pages]"
						value="<?php print(dumont4wp_filter_list2str($dumont4wp_settings['dumont4wp_exclude_pages'], 'dumont4wp')); ?>" /></td>
			</tr>
		</table>
		<hr />
		<h3><?php _e('Result Options', 'dumont4wp') ?></h3>
		<table class="form-table">
			<tr valign="top">
				<th scope="row" style="width: 200px;"><?php _e('Output Result Info', 'dumont4wp') ?></th>
				<td style="width: 10px; float: left;"><input type="checkbox"
						name="settings[dumont4wp_output_info]" value="1"
						<?php echo dumont4wp_checkCheckbox($dumont4wp_settings['dumont4wp_output_info']); ?> /></td>
				<th scope="row"
					style="width: 200px; float: left; margin-left: 20px;"><?php _e('Output Result Pager', 'dumont4wp') ?></th>
				<td style="width: 10px; float: left;"><input type="checkbox"
						name="settings[dumont4wp_output_pager]" value="1"
						<?php echo dumont4wp_checkCheckbox($dumont4wp_settings['dumont4wp_output_pager']); ?> /></td>
			</tr>

			<tr valign="top">
				<th scope="row" style="width: 200px;"><?php _e('Output Facets', 'dumont4wp') ?></th>
				<td style="width: 10px; float: left;"><input type="checkbox"
						name="settings[dumont4wp_output_facets]" value="1"
						<?php echo dumont4wp_checkCheckbox($dumont4wp_settings['dumont4wp_output_facets']); ?> /></td>
				<th scope="row"
					style="width: 200px; float: left; margin-left: 20px;"><?php _e('Category Facet as Taxonomy', 'dumont4wp') ?></th>
				<td style="width: 10px; float: left;"><input type="checkbox"
						name="settings[dumont4wp_cat_as_taxo]" value="1"
						<?php echo dumont4wp_checkCheckbox($dumont4wp_settings['dumont4wp_cat_as_taxo']); ?> /></td>
			</tr>

			<tr valign="top">
				<th scope="row" style="width: 200px;"><?php _e('Categories as Facet', 'dumont4wp') ?></th>
				<td style="width: 10px; float: left;"><input type="checkbox"
						name="settings[dumont4wp_facet_on_categories]" value="1"
						<?php echo dumont4wp_checkCheckbox($dumont4wp_settings['dumont4wp_facet_on_categories']); ?> /></td>
				<th scope="row"
					style="width: 200px; float: left; margin-left: 20px;"><?php _e('Tags as Facet', 'dumont4wp') ?></th>
				<td style="width: 10px; float: left;"><input type="checkbox"
						name="settings[dumont4wp_facet_on_tags]" value="1"
						<?php echo dumont4wp_checkCheckbox($dumont4wp_settings['dumont4wp_facet_on_tags']); ?> /></td>
			</tr>

			<tr valign="top">
				<th scope="row" style="width: 200px;"><?php _e('Author as Facet', 'dumont4wp') ?></th>
				<td style="width: 10px; float: left;"><input type="checkbox"
						name="settings[dumont4wp_facet_on_author]" value="1"
						<?php echo dumont4wp_checkCheckbox($dumont4wp_settings['dumont4wp_facet_on_author']); ?> /></td>
				<th scope="row"
					style="width: 200px; float: left; margin-left: 20px;"><?php _e('Type as Facet', 'dumont4wp') ?></th>
				<td style="width: 10px; float: left;"><input type="checkbox"
						name="settings[dumont4wp_facet_on_type]" value="1"
						<?php echo dumont4wp_checkCheckbox($dumont4wp_settings['dumont4wp_facet_on_type']); ?> /></td>
			</tr>

			<tr valign="top">
				<th scope="row" style="width: 200px;"><?php _e('Taxonomy as Facet', 'dumont4wp') ?></th>
				<td style="width: 10px; float: left;"><input type="checkbox"
						name="settings[dumont4wp_facet_on_taxonomy]" value="1"
						<?php echo dumont4wp_checkCheckbox($dumont4wp_settings['dumont4wp_facet_on_taxonomy']); ?> /></td>
			</tr>

			<tr valign="top">
				<th scope="row"><?php _e('Custom fields as Facet (comma separated ordered names list)') ?></th>
				<td><input type="text"
						name="settings[dumont4wp_facet_on_custom_fields]"
						value="<?php print(dumont4wp_filter_list2str($dumont4wp_settings['dumont4wp_facet_on_custom_fields'], 'dumont4wp')); ?>" /></td>
			</tr>

			<tr valign="top">
				<th scope="row" style="width: 200px;"><?php _e('Enable Spellchecking', 'dumont4wp') ?></th>
				<td style="width: 10px; float: left;"><input type="checkbox"
						name="settings[dumont4wp_enable_dym]" value="1"
						<?php echo dumont4wp_checkCheckbox($dumont4wp_settings['dumont4wp_enable_dym']); ?> /></td>
			</tr>

			<tr valign="top">
				<th scope="row"><?php _e('Number of Results Per Page', 'dumont4wp') ?></th>
				<td><input type="text" name="settings[dumont4wp_num_results]"
						value="<?php _e($dumont4wp_settings['dumont4wp_num_results'], 'dumont4wp'); ?>" /></td>
			</tr>

			<tr valign="top">
				<th scope="row"><?php _e('Max Number of Tags to Display', 'dumont4wp') ?></th>
				<td><input type="text" name="settings[dumont4wp_max_display_tags]"
						value="<?php _e($dumont4wp_settings['dumont4wp_max_display_tags'], 'dumont4wp'); ?>" /></td>
			</tr>
		</table>
		<hr />
		<?php settings_fields('s4w-options-group'); ?>

		<p class="submit">
			<input type="hidden" name="action" value="update" /> <input
				id="settingsbutton" type="submit" class="button-primary"
				value="<?php _e('Save Changes', 'dumont4wp') ?>" />
		</p>

	</form>
	<hr />
	<form method="post"
		action="options-general.php?page=dumont4wp/viglet-dumont-for-wordpress.php">
		<h3><?php _e('Actions', 'dumont4wp') ?></h3>
		<table class="form-table">
			<tr valign="top">
				<th scope="row"><?php _e('Check Server Settings', 'dumont4wp') ?></th>
				<td><input type="submit" class="button-primary"
						name="dumont4wp_ping" value="<?php _e('Execute', 'dumont4wp') ?>" /></td>
			</tr>

			<?php if (is_multisite()) { ?>
				<tr valign="top">
					<th scope="row"><?php _e('Push Solr Configuration to All Blogs', 'dumont4wp') ?></th>
					<td><input type="submit" class="button-primary"
							name="dumont4wp_init_blogs"
							value="<?php _e('Execute', 'dumont4wp') ?>" /></td>
				</tr>
			<?php } ?>

			<?php

			foreach ($post_types as $post_key => $post_type) {
				if ($dumont4wp_settings['dumont4wp_content']['index'][$post_type] == 1) {
			?>

					<tr valign="top">
						<th scope="row"><?php _e('Index all ' . ucfirst($post_type), 'dumont4wp') ?></th>
						<td><input type="submit" class="button-primary content_load"
								name="dumont4wp_content_load[<?php echo $post_type ?>]"
								value="<?php _e('Execute', 'dumont4wp') ?>" /></td>
					</tr>
				<?php
				}
			}

			if (count($dumont4wp_settings['dumont4wp_content']['index']) > 0) {
				?>
				<tr valign="top">
					<th scope="row"><?php _e('Index All Content', 'dumont4wp') ?></th>
					<td><input type="submit" class="button-primary content_load"
							name="dumont4wp_content_load[all]"
							value="<?php _e('Execute', 'dumont4wp') ?>" /></td>
				</tr>
			<?php } ?>

			<tr valign="top">
				<th scope="row"><?php _e('Delete All', 'dumont4wp') ?></th>
				<td><input type="submit" class="button-primary"
						name="dumont4wp_deleteall"
						value="<?php _e('Execute', 'dumont4wp') ?>" /></td>
			</tr>
		</table>
	</form>

</div>