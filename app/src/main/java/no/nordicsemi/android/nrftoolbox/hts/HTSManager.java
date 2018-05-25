/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package no.nordicsemi.android.nrftoolbox.hts;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Calendar;
import java.util.UUID;

import no.nordicsemi.android.ble.common.callback.ht.TemperatureMeasurementDataCallback;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.log.LogContract;
import no.nordicsemi.android.nrftoolbox.battery.BatteryManager;
import no.nordicsemi.android.nrftoolbox.parser.TemperatureMeasurementParser;

/**
 * HTSManager class performs BluetoothGatt operations for connection, service discovery, enabling
 * indication and reading characteristics. All operations required to connect to device with BLE HT
 * Service and reading health thermometer values are performed here.
 * HTSActivity implements HTSManagerCallbacks in order to receive callbacks of BluetoothGatt operations.
 */
public class HTSManager extends BatteryManager<HTSManagerCallbacks> {
	private static final String TAG = "HTSManager";

	/** Health Thermometer service UUID */
	public final static UUID HT_SERVICE_UUID = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb");
	/** Health Thermometer Measurement characteristic UUID */
	private static final UUID HT_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A1C-0000-1000-8000-00805f9b34fb");

	private BluetoothGattCharacteristic mHTCharacteristic;

	HTSManager(final Context context) {
		super(context);
	}

	@NonNull
	@Override
	protected BatteryManagerGattCallback getGattCallback() {
		return mGattCallback;
	}

	/**
	 * BluetoothGatt callbacks for connection/disconnection, service discovery, receiving indication, etc.
	 */
	private final BatteryManagerGattCallback mGattCallback = new BatteryManagerGattCallback() {
		@Override
		protected void initialize() {
			super.initialize();
			setIndicationCallback(mHTCharacteristic)
					.with(new TemperatureMeasurementDataCallback() {
						@Override
						public void onDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
							log(LogContract.Log.Level.APPLICATION, "\"" + TemperatureMeasurementParser.parse(data) + "\" received");
							super.onDataReceived(device, data);
						}

						@Override
						public void onTemperatureMeasurementReceived(@NonNull final BluetoothDevice device,
																	 final float temperature, final int unit,
																	 @Nullable final Calendar calendar,
																	 @Nullable final Integer type) {
							mCallbacks.onTemperatureMeasurementReceived(device, temperature, unit, calendar, type);
						}
					});
			enableIndications(mHTCharacteristic);
		}

		@Override
		protected boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
			final BluetoothGattService service = gatt.getService(HT_SERVICE_UUID);
			if (service != null) {
				mHTCharacteristic = service.getCharacteristic(HT_MEASUREMENT_CHARACTERISTIC_UUID);
			}
			return mHTCharacteristic != null;
		}

		@Override
		protected void onDeviceDisconnected() {
			super.onDeviceDisconnected();
			mHTCharacteristic = null;
		}
	};
}
