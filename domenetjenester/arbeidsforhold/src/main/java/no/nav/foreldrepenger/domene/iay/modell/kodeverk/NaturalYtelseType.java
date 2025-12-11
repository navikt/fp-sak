package no.nav.foreldrepenger.domene.iay.modell.kodeverk;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.MedOffisiellKode;

public enum NaturalYtelseType implements Kodeverdi, MedOffisiellKode {

    ELEKTRISK_KOMMUNIKASJON("ELEKTRISK_KOMMUNIKASJON", "Elektrisk kommunikasjon", "elektroniskKommunikasjon"),
    AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS("AKSJER_UNDERKURS", "Aksjer grunnfondsbevis til underkurs", "aksjerGrunnfondsbevisTilUnderkurs"),
    LOSJI("LOSJI", "Losji", "losji"),
    KOST_DØGN("KOST_DOEGN", "Kostpenger døgnsats", "kostDoegn"),
    BESØKSREISER_HJEMMET_ANNET("BESOEKSREISER_HJEM", "Besøksreiser hjemmet annet", "besoeksreiserHjemmetAnnet"),
    KOSTBESPARELSE_I_HJEMMET("KOSTBESPARELSE_HJEM", "Kostbesparelser i hjemmet", "kostbesparelseIHjemmet"),
    RENTEFORDEL_LÅN("RENTEFORDEL_LAAN", "Rentefordel lån", "rentefordelLaan"),
    BIL("BIL", "Bil", "bil"),
    KOST_DAGER("KOST_DAGER", "Kostpenger dager", "kostDager"),
    BOLIG("BOLIG", "Bolig", "bolig"),
    SKATTEPLIKTIG_DEL_FORSIKRINGER("FORSIKRINGER", "Skattepliktig del forsikringer", "skattepliktigDelForsikringer"),
    FRI_TRANSPORT("FRI_TRANSPORT", "Fri transport", "friTransport"),
    OPSJONER("OPSJONER", "Opsjoner", "opsjoner"),
    TILSKUDD_BARNEHAGEPLASS("TILSKUDD_BARNEHAGE", "Tilskudd barnehageplass", "tilskuddBarnehageplass"),
    ANNET("ANNET", "Annet", "annet"),
    BEDRIFTSBARNEHAGEPLASS("BEDRIFTSBARNEHAGE", "Bedriftsbarnehageplass", "bedriftsbarnehageplass"),
    YRKEBIL_TJENESTLIGBEHOV_KILOMETER("YRKESBIL_KILOMETER", "Yrkesbil tjenesteligbehov kilometer", "yrkebilTjenestligbehovKilometer"),
    YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS("YRKESBIL_LISTEPRIS", "Yrkesbil tjenesteligbehov listepris", "yrkebilTjenestligbehovListepris"),
    INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING("UTENLANDSK_PENSJONSORDNING", "Innbetaling utenlandsk pensjonsordning", "innbetalingTilUtenlandskPensjonsordning"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert", null),
    ;

    private static final Map<String, NaturalYtelseType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String navn;
    @JsonValue
    private final String kode;
    private final String offisiellKode;

    NaturalYtelseType(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    public static NaturalYtelseType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent NaturalYtelseType: " + kode);
        }
        return ad;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

    public static NaturalYtelseType finnForKodeverkEiersKode(String offisiellDokumentType) {
        return Stream.of(values()).filter(k -> Objects.equals(k.offisiellKode, offisiellDokumentType)).findFirst().orElse(UDEFINERT);
    }

}
