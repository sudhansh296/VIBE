package com.vibe.app

import com.vibe.app.ui.screens.host.findIdNumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The number read from an ID card photo (text as the on-phone reader returns it). */
class IdNumberTest {
    @Test fun aadhaarWithChecksum() {
        assertEquals("2345 6789 0124", findIdNumber("aadhaar", "Government of India\nDOB: 01/01/2000\n2345 6789 0124\nVID: 9134 5678 9012 3456"))
        assertNull("a wrong checksum is not taken", findIdNumber("aadhaar", "1234 5678 9012"))
        assertEquals("letter O read instead of zero", "2345 6789 0124", findIdNumber("aadhaar", "2345 6789 O124"))
    }
    @Test fun otherDocuments() {
        assertEquals("ABC1234567", findIdNumber("voter_id", "ELECTION COMMISSION\nABC1234567\nName: Test"))
        assertEquals("K1234567", findIdNumber("passport", "REPUBLIC OF INDIA\nPassport No. K1234567"))
        assertEquals("MH1420110062821", findIdNumber("driving_licence", "DL No: MH14 20110062821"))
        assertNull(findIdNumber("passport", "no number here"))
    }
}
