package no.nav.foreldrepenger.behandlingslager.virksomhet;

/**
 * Typer av arbeidsforhold.
 * <p>
 * <h3>Kilde: Nav kodeverk</h3>
 * https://modapp.adeo.no/kodeverksklient/viskodeverk/Arbeidsforholdstyper/2
 * <p>
 * <h3>Tjeneste(r) som returnerer dette:</h3>
 * <ul>
 * <li>https://confluence.adeo.no/display/SDFS/tjeneste_v3%3Avirksomhet%3AArbeidsforhold_v3</li>
 * </ul>
 * <h3>Tjeneste(r) som konsumerer dete:</h3>
 * <ul>
 * <li></li>
 * </ul>
 */

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.MedOffisiellKode;

public enum ArbeidType implements Kodeverdi, MedOffisiellKode {

    ETTERLØNN_SLUTTPAKKE("ETTERLØNN_SLUTTPAKKE", "Etterlønn eller sluttpakke"),
    FORENKLET_OPPGJØRSORDNING("FORENKLET_OPPGJØRSORDNING", "Forenklet oppgjørsordning ", "forenkletOppgjoersordning"),
    FRILANSER("FRILANSER", "Frilanser, samlet aktivitet"),
    FRILANSER_OPPDRAGSTAKER_MED_MER("FRILANSER_OPPDRAGSTAKER", "Frilansere/oppdragstakere, med mer", "frilanserOppdragstakerHonorarPersonerMm"),
    LØNN_UNDER_UTDANNING("LØNN_UNDER_UTDANNING", "Lønn under utdanning"),
    MARITIMT_ARBEIDSFORHOLD("MARITIMT_ARBEIDSFORHOLD", "Maritimt arbeidsforhold", "maritimtArbeidsforhold"),
    MILITÆR_ELLER_SIVILTJENESTE("MILITÆR_ELLER_SIVILTJENESTE", "Militær eller siviltjeneste"),
    ORDINÆRT_ARBEIDSFORHOLD("ORDINÆRT_ARBEIDSFORHOLD", "Ordinært arbeidsforhold", "ordinaertArbeidsforhold"),
    PENSJON_OG_ANDRE_TYPER_YTELSER_UTEN_ANSETTELSESFORHOLD("PENSJON_OG_ANDRE_TYPER_YTELSER_UTEN_ANSETTELSESFORHOLD", "Pensjoner og andre typer ytelser",
            "pensjonOgAndreTyperYtelserUtenAnsettelsesforhold"),
    SELVSTENDIG_NÆRINGSDRIVENDE("NÆRING", "Selvstendig næringsdrivende"),
    UTENLANDSK_ARBEIDSFORHOLD("UTENLANDSK_ARBEIDSFORHOLD", "Arbeid i utlandet"),
    VENTELØNN_VARTPENGER("VENTELØNN_VARTPENGER", "Ventelønn eller vartpenger"),
    VANLIG("VANLIG", "Vanlig", "VANLIG"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert"),
    ;

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<ArbeidType, String> {
        @Override
        public String convertToDatabaseColumn(ArbeidType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public ArbeidType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

    }

    private static final Set<ArbeidType> ANNEN_OPPTJENING = Set.of(
        FRILANSER, MILITÆR_ELLER_SIVILTJENESTE, UTENLANDSK_ARBEIDSFORHOLD,
        ETTERLØNN_SLUTTPAKKE, LØNN_UNDER_UTDANNING, VENTELØNN_VARTPENGER);

    public static final Set<ArbeidType> AA_REGISTER_TYPER = Set.of(
        ArbeidType.ORDINÆRT_ARBEIDSFORHOLD,
        ArbeidType.MARITIMT_ARBEIDSFORHOLD,
        ArbeidType.FORENKLET_OPPGJØRSORDNING);

    private static final Map<String, ArbeidType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonValue
    private final String kode;
    @JsonIgnore
    private final String navn;
    @JsonIgnore
    private final String offisiellKode;

    ArbeidType(String kode, String navn) {
        this(kode, navn, null);
    }

    ArbeidType(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    public static ArbeidType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent ArbeidType: " + kode);
        }
        return ad;
    }

    public boolean erAnnenOpptjening() {
        return ANNEN_OPPTJENING.contains(this);
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

}
