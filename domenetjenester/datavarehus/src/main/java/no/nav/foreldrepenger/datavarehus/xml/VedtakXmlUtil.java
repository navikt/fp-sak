package no.nav.foreldrepenger.datavarehus.xml;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.Calendar;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.xmlutils.DateUtil;
import no.nav.vedtak.felles.xml.felles.v2.BooleanOpplysning;
import no.nav.vedtak.felles.xml.felles.v2.DateOpplysning;
import no.nav.vedtak.felles.xml.felles.v2.DecimalOpplysning;
import no.nav.vedtak.felles.xml.felles.v2.DoubleOpplysning;
import no.nav.vedtak.felles.xml.felles.v2.FloatOpplysning;
import no.nav.vedtak.felles.xml.felles.v2.IntOpplysning;
import no.nav.vedtak.felles.xml.felles.v2.KodeverksOpplysning;
import no.nav.vedtak.felles.xml.felles.v2.LongOpplysning;
import no.nav.vedtak.felles.xml.felles.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.felles.v2.PeriodeOpplysning;
import no.nav.vedtak.felles.xml.felles.v2.StringOpplysning;

public class VedtakXmlUtil {
    private static ObjectFactory fellesObjectFactory = new ObjectFactory();

    private VedtakXmlUtil() {
    }


    public static Optional<DateOpplysning> lagDateOpplysning(LocalDate localDate) {
        if (localDate == null) {
            return Optional.empty();
        }
        var dateOpplysning = fellesObjectFactory.createDateOpplysning();
        dateOpplysning.setValue(localDate);
        return Optional.of(dateOpplysning);
    }

    public static StringOpplysning lagStringOpplysning(String str) {
        var stringOpplysning = fellesObjectFactory.createStringOpplysning();
        stringOpplysning.setValue(str);
        return stringOpplysning;
    }

    /**
     * Lager string representasjon av perioden.
     */
    public static StringOpplysning lagStringOpplysningForperiode(Period periode) {
        var stringOpplysning = fellesObjectFactory.createStringOpplysning();
        if (Objects.nonNull(periode)) {
            stringOpplysning.setValue(periode.toString());
        }
        return stringOpplysning;
    }

    public static DoubleOpplysning lagDoubleOpplysning(double value) {
        var doubleOpplysning = fellesObjectFactory.createDoubleOpplysning();
        doubleOpplysning.setValue(value);
        return doubleOpplysning;
    }

    public static FloatOpplysning lagFloatOpplysning(float value) {
        var floatOpplysning = fellesObjectFactory.createFloatOpplysning();
        floatOpplysning.setValue(value);
        return floatOpplysning;
    }

    public static LongOpplysning lagLongOpplysning(long value) {
        var longOpplysning = fellesObjectFactory.createLongOpplysning();
        longOpplysning.setValue(value);
        return longOpplysning;
    }

    public static PeriodeOpplysning lagPeriodeOpplysning(LocalDate fom, LocalDate tom) {
        var periodeOpplysning = fellesObjectFactory.createPeriodeOpplysning();
        periodeOpplysning.setFom(fom);
        periodeOpplysning.setTom(tom);
        return periodeOpplysning;
    }

    public static KodeverksOpplysning lagTomKodeverksOpplysning() {
        return fellesObjectFactory.createKodeverksOpplysning();
    }

    public static KodeverksOpplysning lagKodeverksOpplysning(Kodeverdi kodeverdi) {
        var kode = kodeverdi.getKode();
        var navn = kodeverdi.getNavn();
        var kodeverk = kodeverdi.getClass().getSimpleName();
        return lagKodeverksOpplysning(kode, navn, kodeverk);
    }

    public static KodeverksOpplysning lagKodeverksOpplysning(String kode, String navn, String kodeverk) {
        var kodeverksOpplysning = fellesObjectFactory.createKodeverksOpplysning();
        kodeverksOpplysning.setKode(kode);
        kodeverksOpplysning.setValue(navn);
        kodeverksOpplysning.setKodeverk(kodeverk);
        return kodeverksOpplysning;
    }

    public static BooleanOpplysning lagBooleanOpplysning(Boolean bool) {
        if (Objects.isNull(bool)) {
            return null;
        }
        var booleanOpplysning = fellesObjectFactory.createBooleanOpplysning();
        booleanOpplysning.setValue(bool);
        return booleanOpplysning;
    }

    public static IntOpplysning lagIntOpplysning(int value) {
        var intOpplysning = fellesObjectFactory.createIntOpplysning();
        intOpplysning.setValue(value);
        return intOpplysning;
    }

    public static DecimalOpplysning lagDecimalOpplysning(BigDecimal value) {
        var decimalOpplysning = fellesObjectFactory.createDecimalOpplysning();
        decimalOpplysning.setValue(value);
        return decimalOpplysning;
    }

    public static Calendar tilCalendar(LocalDate localDate) {
        return DateUtil.convertToXMLGregorianCalendarRemoveTimezone(localDate).toGregorianCalendar();
    }

    public static KodeverksOpplysning lagKodeverksOpplysningForAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        var kodeverksOpplysning = fellesObjectFactory.createKodeverksOpplysning();
        kodeverksOpplysning.setValue(aksjonspunktDefinisjon.getNavn());
        kodeverksOpplysning.setKode(aksjonspunktDefinisjon.getKode());
        kodeverksOpplysning.setKodeverk("AKSJONSPUNKT_DEF");
        return kodeverksOpplysning;
    }
}
