package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum StønadskontoType implements Kodeverdi {

    /*
     * Alle kvanta av dager som er definert i Ftl 14-9 ... 14-15 fom 1/1-2019
     * - De fleste er gitt av rettighetssituasjon (Aleneomsorg, Ene eller Begge rette), valgt dekningsgrad og antall barn
     * - Enkelte er gitt av omstendigheter (prematur og tette saker/fødsler)
     * - Noen definerer far/medmors rettigheter
     * - De fleste har vært tilstede siden FPsak ble laget. Tillegg er prematur og fars delrettigheter
     *
     * Brukskonvensjon:
     * - Tas med dersom de er utregnet til >0 dager
     * - Vil bare unntaksvis endres når først regnet ut (dekningsgrad, prematuritet, uføretrygd)
     */

    FELLESPERIODE("FELLESPERIODE", "Fellesperiode", KontoKategori.STØNADSDAGER),
    MØDREKVOTE("MØDREKVOTE", "Mødrekvote", KontoKategori.STØNADSDAGER),
    FEDREKVOTE("FEDREKVOTE", "Fedrekvote", KontoKategori.STØNADSDAGER),
    FORELDREPENGER("FORELDREPENGER", "Foreldrepenger", KontoKategori.STØNADSDAGER),
    FORELDREPENGER_FØR_FØDSEL("FORELDREPENGER_FØR_FØDSEL", "Foreldrepenger før fødsel", KontoKategori.STØNADSDAGER),

    TILLEGG_FLERBARN("TILLEGG_FLERBARN", "Tilleggsdager ved flerbarnstilfelle", KontoKategori.UTVIDELSE), // Jfr Ftl 14-9
    TILLEGG_PREMATUR("TILLEGG_PREMATUR", "Tilleggsdager ved prematur fødsel", KontoKategori.UTVIDELSE), // Jfr Ftl 14-10 a

    FLERBARNSDAGER("FLERBARNSDAGER", "Flerbarnsdager", KontoKategori.AKTIVITETSKRAV), // Kan tas uten krav til aktivitet, jfr Ftl 14-13
    UFØREDAGER("UFØREDAGER", "Uføredager", KontoKategori.AKTIVITETSKRAV), // Kan tas uten krav til aktivitet, jfr Ftl 14-13. Før WLB-direktiv

    TETTE_SAKER_MOR("TETTE_SAKER_MOR", "Tette fødsler mor", KontoKategori.MINSTERETT),
    TETTE_SAKER_FAR("TETTE_SAKER_FAR", "Tette fødsler far/medmor", KontoKategori.MINSTERETT),
    BARE_FAR_RETT("BARE_FAR_RETT", "Bare far har rett", KontoKategori.MINSTERETT),

    // Annet, bla dager med fullt (200%) samtidig uttak (se også flerbarnsdager)
    FAR_RUNDT_FØDSEL("FAR_RUNDT_FØDSEL", "Fars uttak ifm fødsel", KontoKategori.ANNET),

    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke valgt stønadskonto", KontoKategori.ANNET),
    ;

    public enum KontoKategori { STØNADSDAGER, UTVIDELSE, AKTIVITETSKRAV, MINSTERETT, ANNET }

    private static final Map<String, StønadskontoType> KODER = new LinkedHashMap<>();

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

    private final KontoKategori kategori;

    StønadskontoType(String kode, String navn, KontoKategori kategori) {
        this.kode = kode;
        this.navn = navn;
        this.kategori = kategori;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

    public KontoKategori getKategori() {
        return kategori;
    }

    public boolean erStønadsdager() {
        return KontoKategori.STØNADSDAGER.equals(this.kategori);
    }

    public UttakPeriodeType toUttakPeriodeType() {
        return switch (this) {
            case FELLESPERIODE -> UttakPeriodeType.FELLESPERIODE;
            case MØDREKVOTE -> UttakPeriodeType.MØDREKVOTE;
            case FEDREKVOTE -> UttakPeriodeType.FEDREKVOTE;
            case FORELDREPENGER -> UttakPeriodeType.FORELDREPENGER;
            case FORELDREPENGER_FØR_FØDSEL -> UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL;
            case UDEFINERT -> UttakPeriodeType.UDEFINERT;
            default -> throw new IllegalStateException("Unexpected value: " + this);
        };
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<StønadskontoType, String> {
        @Override
        public String convertToDatabaseColumn(StønadskontoType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public StønadskontoType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static StønadskontoType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent StønadskontoType: " + kode);
            }
            return ad;
        }
    }
}
