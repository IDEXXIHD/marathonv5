# encoding: utf-8
#
# Licensed to the Software Freedom Conservancy (SFC) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The SFC licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

module Selenium
  module WebDriver
    module Remote
      #
      # Low level bridge to the remote server, through which the rest of the API works.
      #
      # @api private
      #

      class Bridge
        include BridgeHelper

        attr_accessor :context, :http, :file_detector
        attr_reader :capabilities

        #
        # Initializes the bridge with the given server URL.
        #
        # @param url         [String] url for the remote server
        # @param http_client [Object] an HTTP client instance that implements the same protocol as Http::Default
        # @param desired_capabilities [Capabilities] an instance of Remote::Capabilities describing the capabilities you want
        #

        def initialize(opts = {})
          opts = opts.dup

          port = opts.delete(:port) || 4444
          http_client = opts.delete(:http_client) { Http::Default.new }
          desired_capabilities = opts.delete(:desired_capabilities) { Capabilities.firefox }
          url = opts.delete(:url) { "http://#{Platform.localhost}:#{port}/wd/hub" }

          unless opts.empty?
            raise ArgumentError, "unknown option#{'s' if opts.size != 1}: #{opts.inspect}"
          end

          if desired_capabilities.is_a?(Symbol)
            unless Capabilities.respond_to?(desired_capabilities)
              raise Error::WebDriverError, "invalid desired capability: #{desired_capabilities.inspect}"
            end

            desired_capabilities = Capabilities.send(desired_capabilities)
          end

          uri = url.is_a?(URI) ? url : URI.parse(url)
          uri.path += '/' unless uri.path =~ %r{\/$}

          http_client.server_url = uri

          @http = http_client
          @capabilities = create_session(desired_capabilities)

          @file_detector = nil
        end

        def browser
          @browser ||= (
            name = @capabilities.browser_name
            name ? name.tr(' ', '_').to_sym : 'unknown'
          )
        end

        def driver_extensions
          [
            DriverExtensions::HasInputDevices,
            DriverExtensions::UploadsFiles,
            DriverExtensions::TakesScreenshot,
            DriverExtensions::HasSessionId,
            DriverExtensions::Rotatable,
            DriverExtensions::HasTouchScreen,
            DriverExtensions::HasLocation,
            DriverExtensions::HasNetworkConnection,
            DriverExtensions::HasRemoteStatus,
            DriverExtensions::HasWebStorage
          ]
        end

        def commands(command)
          COMMANDS[command]
        end

        #
        # Returns the current session ID.
        #

        def session_id
          @session_id || raise(Error::WebDriverError, 'no current session exists')
        end

        def create_session(desired_capabilities)
          resp = raw_execute :new_session, {}, {desiredCapabilities: desired_capabilities}
          @session_id = resp['sessionId']
          return Capabilities.json_create resp['value'] if @session_id

          raise Error::WebDriverError, 'no sessionId in returned payload'
        end

        def status
          execute :status
        end

        def get(url)
          execute :get, {}, {url: url}
        end

        def session_capabilities
          Capabilities.json_create execute(:get_capabilities)
        end

        def implicit_wait_timeout=(milliseconds)
          execute :implicitly_wait, {}, {ms: milliseconds}
        end

        def script_timeout=(milliseconds)
          execute :set_script_timeout, {}, {ms: milliseconds}
        end

        def timeout(type, milliseconds)
          execute :set_timeout, {}, {type: type, ms: milliseconds}
        end

        #
        # alerts
        #

        def accept_alert
          execute :accept_alert
        end

        def dismiss_alert
          execute :dismiss_alert
        end

        def alert=(keys)
          execute :set_alert_value, {}, {text: keys.to_s}
        end

        def alert_text
          execute :get_alert_text
        end

        def authentication(credentials)
          execute :set_authentication, {}, credentials
        end

        #
        # navigation
        #

        def go_back
          execute :go_back
        end

        def go_forward
          execute :go_forward
        end

        def url
          execute :get_current_url
        end

        def title
          execute :get_title
        end

        def page_source
          execute :get_page_source
        end

        def switch_to_window(name)
          execute :switch_to_window, {}, {name: name}
        end

        def switch_to_frame(id)
          execute :switch_to_frame, {}, {id: id}
        end

        def switch_to_parent_frame
          execute :switch_to_parent_frame
        end

        def switch_to_default_content
          switch_to_frame(nil)
        end

        QUIT_ERRORS = [IOError].freeze

        def quit
          execute :quit
          http.close
        rescue *QUIT_ERRORS
        end

        def close
          execute :close
        end

        def refresh
          execute :refresh
        end

        #
        # window handling
        #

        def window_handles
          execute :get_window_handles
        end

        def window_handle
          execute :get_current_window_handle
        end

        def resize_window(width, height, handle = :current)
          execute :set_window_size, {window_handle: handle},
                  {width: width,
                   height: height}
        end

        def maximize_window(handle = :current)
          execute :maximize_window, window_handle: handle
        end

        def window_size(handle = :current)
          data = execute :get_window_size, window_handle: handle

          Dimension.new data['width'], data['height']
        end

        def reposition_window(x, y, handle = :current)
          execute :set_window_position, {window_handle: handle},
                  {x: x, y: y}
        end

        def window_position(handle = :current)
          data = execute :get_window_position, window_handle: handle

          Point.new data['x'], data['y']
        end

        def screenshot
          execute :screenshot
        end

        #
        # HTML 5
        #

        def local_storage_item(key, value = nil)
          if value
            execute :set_local_storage_item, {}, {key: key, value: value}
          else
            execute :get_local_storage_item, key: key
          end
        end

        def remove_local_storage_item(key)
          execute :remove_local_storage_item, key: key
        end

        def local_storage_keys
          execute :get_local_storage_keys
        end

        def clear_local_storage
          execute :clear_local_storage
        end

        def local_storage_size
          execute :get_local_storage_size
        end

        def session_storage_item(key, value = nil)
          if value
            execute :set_session_storage_item, {}, {key: key, value: value}
          else
            execute :get_session_storage_item, key: key
          end
        end

        def remove_session_storage_item(key)
          execute :remove_session_storage_item, key: key
        end

        def session_storage_keys
          execute :get_session_storage_keys
        end

        def clear_session_storage
          execute :clear_session_storage
        end

        def session_storage_size
          execute :get_session_storage_size
        end

        def location
          obj = execute(:get_location) || {}
          Location.new obj['latitude'], obj['longitude'], obj['altitude']
        end

        def set_location(lat, lon, alt)
          loc = {latitude: lat, longitude: lon, altitude: alt}
          execute :set_location, {}, {location: loc}
        end

        def network_connection
          execute :get_network_connection
        end

        def network_connection=(type)
          execute :set_network_connection, {}, {parameters: {type: type}}
        end

        #
        # javascript execution
        #

        def execute_script(script, *args)
          assert_javascript_enabled

          result = execute :execute_script, {}, {script: script, args: args}
          unwrap_script_result result
        end

        def execute_async_script(script, *args)
          assert_javascript_enabled

          result = execute :execute_async_script, {}, {script: script, args: args}
          unwrap_script_result result
        end

        #
        # cookies
        #

        def options
          @options ||= WebDriver::Options.new(self)
        end

        def add_cookie(cookie)
          execute :add_cookie, {}, {cookie: cookie}
        end

        def delete_cookie(name)
          execute :delete_cookie, name: name
        end

        def cookies
          execute :get_cookies
        end

        def delete_all_cookies
          execute :delete_all_cookies
        end

        #
        # actions
        #

        def click_element(element)
          execute :click_element, id: element
        end

        def click
          execute :click, {}, {button: 0}
        end

        def double_click
          execute :double_click
        end

        def context_click
          execute :click, {}, {button: 2}
        end

        def mouse_down
          execute :mouse_down
        end

        def mouse_up
          execute :mouse_up
        end

        def mouse_move_to(element, x = nil, y = nil)
          params = {element: element}

          if x && y
            params[:xoffset] = x
            params[:yoffset] = y
          end

          execute :mouse_move_to, {}, params
        end

        def send_keys_to_active_element(key)
          execute :send_keys_to_active_element, {}, {value: key}
        end

        def send_keys_to_element(element, keys)
          if @file_detector
            local_file = @file_detector.call(keys)
            keys = upload(local_file) if local_file
          end

          execute :send_keys_to_element, {id: element}, {value: Array(keys)}
        end

        def upload(local_file)
          unless File.file?(local_file)
            raise Error::WebDriverError, "you may only upload files: #{local_file.inspect}"
          end

          execute :upload_file, {}, {file: Zipper.zip_file(local_file)}
        end

        def clear_element(element)
          execute :clear_element, id: element
        end

        def submit_element(element)
          execute :submit_element, id: element
        end

        def drag_element(element, right_by, down_by)
          execute :drag_element, {id: element}, {x: right_by, y: down_by}
        end

        def touch_single_tap(element)
          execute :touch_single_tap, {}, {element: element}
        end

        def touch_double_tap(element)
          execute :touch_double_tap, {}, {element: element}
        end

        def touch_long_press(element)
          execute :touch_long_press, {}, {element: element}
        end

        def touch_down(x, y)
          execute :touch_down, {}, {x: x, y: y}
        end

        def touch_up(x, y)
          execute :touch_up, {}, {x: x, y: y}
        end

        def touch_move(x, y)
          execute :touch_move, {}, {x: x, y: y}
        end

        def touch_scroll(element, x, y)
          if element
            execute :touch_scroll, {}, {element: element,
                                       xoffset: x,
                                       yoffset: y}
          else
            execute :touch_scroll, {}, {xoffset: x, yoffset: y}
          end
        end

        def touch_flick(xspeed, yspeed)
          execute :touch_flick, {}, {xspeed: xspeed, yspeed: yspeed}
        end

        def touch_element_flick(element, right_by, down_by, speed)
          execute :touch_flick, {}, {element: element,
                                    xoffset: right_by,
                                    yoffset: down_by,
                                    speed: speed}
        end

        def screen_orientation=(orientation)
          execute :set_screen_orientation, {}, {orientation: orientation}
        end

        def screen_orientation
          execute :get_screen_orientation
        end

        #
        # logs
        #

        def available_log_types
          types = execute :get_available_log_types
          Array(types).map(&:to_sym)
        end

        def log(type)
          data = execute :get_log, {}, {type: type.to_s}

          Array(data).map do |l|
            begin
              LogEntry.new l.fetch('level', 'UNKNOWN'), l.fetch('timestamp'), l.fetch('message')
            rescue KeyError
              next
            end
          end
        end

        #
        # element properties
        #

        def element_tag_name(element)
          execute :get_element_tag_name, id: element
        end

        def element_attribute(element, name)
          execute :get_element_attribute, id: element.ref, name: name
        end

        # Backwards compatibility for w3c
        def element_property(element, name)
          execute_script 'return arguments[0][arguments[1]]', element, name
        end

        def element_value(element)
          execute :get_element_value, id: element
        end

        def element_text(element)
          execute :get_element_text, id: element
        end

        def element_location(element)
          data = execute :get_element_location, id: element

          Point.new data['x'], data['y']
        end

        def element_location_once_scrolled_into_view(element)
          data = execute :get_element_location_once_scrolled_into_view, id: element

          Point.new data['x'], data['y']
        end

        def element_size(element)
          data = execute :get_element_size, id: element

          Dimension.new data['width'], data['height']
        end

        def element_enabled?(element)
          execute :is_element_enabled, id: element
        end

        def element_selected?(element)
          execute :is_element_selected, id: element
        end

        def element_displayed?(element)
          execute :is_element_displayed, id: element
        end

        def element_value_of_css_property(element, prop)
          execute :get_element_value_of_css_property, id: element, property_name: prop
        end

        #
        # finding elements
        #

        def active_element
          Element.new self, element_id_from(execute(:get_active_element))
        end

        alias_method :switch_to_active_element, :active_element

        def find_element_by(how, what, parent = nil)
          id = if parent
                 execute :find_child_element, {id: parent}, {using: how, value: what}
               else
                 execute :find_element, {}, {using: how, value: what}
               end

          Element.new self, element_id_from(id)
        end

        def find_elements_by(how, what, parent = nil)
          ids = if parent
                  execute :find_child_elements, {id: parent}, {using: how, value: what}
                else
                  execute :find_elements, {}, {using: how, value: what}
                end

          ids.map { |id| Element.new self, element_id_from(id) }
        end

        private

        def assert_javascript_enabled
          return if capabilities.javascript_enabled?
          raise Error::UnsupportedOperationError, 'underlying webdriver instance does not support javascript'
        end

        #
        # executes a command on the remote server.
        #
        #
        # Returns the 'value' of the returned payload
        #

        def execute(*args)
          raw_execute(*args)['value']
        end

        #
        # executes a command on the remote server.
        #
        # @return [WebDriver::Remote::Response]
        #

        def raw_execute(command, opts = {}, command_hash = nil)
          verb, path = commands(command) || raise(ArgumentError, "unknown command: #{command.inspect}")
          path = path.dup

          path[':session_id'] = @session_id if path.include?(':session_id')

          begin
            opts.each { |key, value| path[key.inspect] = escaper.escape(value.to_s) }
          rescue IndexError
            raise ArgumentError, "#{opts.inspect} invalid for #{command.inspect}"
          end

          puts "-> #{verb.to_s.upcase} #{path}" if $DEBUG
          http.call verb, path, command_hash
        end

        def escaper
          @escaper ||= defined?(URI::Parser) ? URI::Parser.new : URI
        end
      end # Bridge
    end # Remote
  end # WebDriver
end # Selenium