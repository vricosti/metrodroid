/*
 * BlankUltralightTransitData.java
 *
 * Copyright 2018 Google
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package au.id.micolous.metrodroid.transit.unknown;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import au.id.micolous.metrodroid.multi.Localizer;
import org.jetbrains.annotations.NonNls;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.ultralight.UltralightCard;
import au.id.micolous.metrodroid.card.ultralight.UltralightCardTransitFactory;
import au.id.micolous.metrodroid.card.ultralight.UltralightPage;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

/**
 * Handle MIFARE Ultralight with no non-default data
 */
public class BlankUltralightTransitData extends TransitData {
    public static final Parcelable.Creator<BlankUltralightTransitData> CREATOR = new Parcelable.Creator<BlankUltralightTransitData>() {
        public BlankUltralightTransitData createFromParcel(Parcel parcel) {
            return new BlankUltralightTransitData(parcel);
        }

        public BlankUltralightTransitData[] newArray(int size) {
            return new BlankUltralightTransitData[size];
        }
    };

    private BlankUltralightTransitData() {

    }

    public final static UltralightCardTransitFactory FACTORY = new UltralightCardTransitFactory() {
        /**
         * @param card Card to read.
         * @return true if all sectors on the card are blank.
         */
        @Override
        public boolean check(@NonNull UltralightCard card) {
            List<UltralightPage> pages = card.getPages();
            @NonNls final String model = card.getCardModel();

            // check to see if all sectors are blocked
            int idx = -1;
            for (UltralightPage p : pages) {
                idx++;
                // Page 2 is serial, internal and lock bytes
                // Page 3 is OTP counters
                // User memory is page 4 and above
                if (idx <= 2) {
                    continue;
                }
                if (p.isUnauthorized()) {
                    // At least one page is "closed", this is not for us
                    return false;
                }
                ImmutableByteArray data = p.getData();
                if (idx == 0x2) {
                    if (data.get(2) != 0 || data.get(3) != 0)
                        return false;
                    continue;
                }

                if (model != null && model.startsWith("NTAG21")) {
                    // Factory-set data on NTAG
                    if (model.equals("NTAG213")) {
                        if (idx == 0x03 && data.contentEquals(new byte[]{(byte) 0xE1, 0x10, 0x12, 0}))
                            continue;
                        if (idx == 0x04 && data.contentEquals(new byte[]{0x01, 0x03, (byte) 0xA0, 0x0C}))
                            continue;
                        if (idx == 0x05 && data.contentEquals(new byte[]{0x34, 0x03, 0, (byte) 0xFE}))
                            continue;
                    }

                    if (model.equals("NTAG215")) {
                        if (idx == 0x03 && data.contentEquals(new byte[]{(byte) 0xE1, 0x10, 0x3E, 0}))
                            continue;
                        if (idx == 0x04 && data.contentEquals(new byte[]{0x03, 0, (byte) 0xFE, 0}))
                            continue;
                        // Page 5 is all null
                    }

                    if (model.equals("NTAG215")) {
                        if (idx == 0x03 && data.contentEquals(new byte[]{(byte) 0xE1, 0x10, 0x6D, 0}))
                            continue;
                        if (idx == 0x04 && data.contentEquals(new byte[]{0x03, 0, (byte) 0xFE, 0}))
                            continue;
                        // Page 5 is all null
                    }

                    // Ignore configuration pages
                    if (idx == pages.size() - 5) {
                        // LOCK BYTE / RFUI
                        // Only care about first three bytes
                        if (data.sliceOffLen(0, 3).contentEquals(new byte[]{0, 0, 0}))
                            continue;
                    }

                    if (idx == pages.size() - 4) {
                        // MIRROR / RFUI / MIRROR_PAGE / AUTH0
                        // STRG_MOD_EN = 1
                        // AUTHO = 0xff
                        if (data.contentEquals(new byte[]{4, 0, 0, (byte) 0xFF}))
                            continue;
                    }

                    if (idx == pages.size() - 3) {
                        // ACCESS / RFUI
                        // Only care about first byte
                        if (data.get(0) == 0)
                            continue;
                    }

                    if (idx <= pages.size() - 2) {
                        // PWD (always masked)
                        // PACK / RFUI
                        continue;
                    }
                } else {
                    // page 0x10 and 0x11 on 384-bit card are config
                    if (pages.size() == 0x14) {
                        if (idx == 0x10 && data.contentEquals(new byte[]{0, 0, 0, -1}))
                            continue;
                        if (idx == 0x11 && data.contentEquals(new byte[]{0, 5, 0, 0}))
                            continue;
                    }
                }

                if (!data.contentEquals(new byte[]{0, 0, 0, 0})) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public TransitData parseTransitData(@NonNull UltralightCard ultralightCard) {
            return new BlankUltralightTransitData();
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull UltralightCard card) {
            return new TransitIdentity(Localizer.INSTANCE.localizeString(R.string.blank_mfu_card), null);
        }
    };

    @Override
    public String getSerialNumber() {
        return null;
    }

    @Override
    public String getCardName() {
        return Localizer.INSTANCE.localizeString(R.string.blank_mfu_card);
    }


    @Override
    public void writeToParcel(Parcel dest, int i) {
    }

    private BlankUltralightTransitData(Parcel p) {
    }
}
