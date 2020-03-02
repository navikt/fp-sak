package no.nav.foreldrepenger.behandlingslager.uttak;

import static no.nav.foreldrepenger.behandlingslager.uttak.StønadskontoType.FEDREKVOTE;
import static no.nav.foreldrepenger.behandlingslager.uttak.StønadskontoType.FELLESPERIODE;
import static no.nav.foreldrepenger.behandlingslager.uttak.StønadskontoType.FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.uttak.StønadskontoType.FORELDREPENGER_FØR_FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.uttak.StønadskontoType.MØDREKVOTE;
import static no.nav.foreldrepenger.behandlingslager.uttak.UttakType.UTSETTELSE;
import static no.nav.foreldrepenger.behandlingslager.uttak.UttakType.UTTAK;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import no.nav.foreldrepenger.behandlingslager.uttak.InnvilgetÅrsak.MyInnvilgetÅrsakPeriodeResultatÅrsakSerializer;
import no.nav.vedtak.konfig.Tid;

@JsonSerialize(using = MyInnvilgetÅrsakPeriodeResultatÅrsakSerializer.class)
@JsonDeserialize(using = PeriodeResultatÅrsakDeserializer.class)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum InnvilgetÅrsak implements PeriodeResultatÅrsak {

    UTTAK_OPPFYLT("2001", "§14-6: Uttak er oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-6\"}}}", Set.of(UTTAK), LocalDate.of(2001, 1, 1)),
    FELLESPERIODE_ELLER_FORELDREPENGER("2002", "§14-9: Innvilget fellesperiode/foreldrepenger", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-9\"}}}", Set.of(UTTAK), Set.of(FELLESPERIODE, FORELDREPENGER)),
    KVOTE_ELLER_OVERFØRT_KVOTE("2003", "§14-12: Innvilget uttak av kvote", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-12\"}}}", Set.of(UTTAK), Set.of(MØDREKVOTE, FEDREKVOTE)),
    FORELDREPENGER_KUN_FAR_HAR_RETT("2004", "§14-14, jf. §14-13 : Innvilget foreldrepenger, kun far har rett", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-14,14-13\"}}}", Set.of(UTTAK), Set.of(FORELDREPENGER)),
    FORELDREPENGER_ALENEOMSORG("2005", "§14-15: Innvilget foreldrepenger ved aleneomsorg", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-15\"}}}", Set.of(UTTAK), Set.of(FORELDREPENGER)),
    INNVILGET_FORELDREPENGER_FØR_FØDSEL("2006", "§14-10: Innvilget foreldrepenger før fødsel", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK), Set.of(FORELDREPENGER_FØR_FØDSEL)),
    FORELDREPENGER_KUN_MOR_HAR_RETT("2007", "§14-10: Innvilget foreldrepenger, kun mor har rett", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK), Set.of(FORELDREPENGER)),
    UTSETTELSE_GYLDIG_PGA_FERIE("2010", "§14-11 første ledd bokstav a: Gyldig utsettelse pga. ferie", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE)),
    UTSETTELSE_GYLDIG_PGA_100_PROSENT_ARBEID("2011", "§14-11 første ledd bokstav b: Gyldig utsettelse pga. 100% arbeid", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE)),
    UTSETTELSE_GYLDIG_PGA_INNLEGGELSE("2012", "§14-11 første ledd bokstav c: Gyldig utsettelse pga. innleggelse", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE)),
    UTSETTELSE_GYLDIG_PGA_BARN_INNLAGT("2013", "§14-11 første ledd bokstav d: Gyldig utsettelse pga. barn innlagt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE)),
    UTSETTELSE_GYLDIG_PGA_SYKDOM("2014", "§14-11 første ledd bokstav c: Gyldig utsettelse pga. sykdom", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE)),
    UTSETTELSE_GYLDIG_PGA_FERIE_KUN_FAR_HAR_RETT("2015", "§14-11 første ledd bokstav a, jf. §14-14, jf. §14-13: Utsettelse pga. ferie, kun far har rett", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,14-14,14-13\"}}}", Set.of(UTSETTELSE)),
    UTSETTELSE_GYLDIG_PGA_ARBEID_KUN_FAR_HAR_RETT("2016", "§14-11 første ledd bokstav b, jf. §14-14, jf. §14-13: Utsettelse pga. 100% arbeid, kun far har rett", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,14-14,14-13\"}}}", Set.of(UTSETTELSE)),
    UTSETTELSE_GYLDIG_PGA_SYKDOM_KUN_FAR_HAR_RETT("2017", "§14-11 første ledd bokstav c, jf. §14-14, jf. §14-13: Utsettelse pga. sykdom, skade, kun far har rett", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,14-14,14-13\"}}}", Set.of(UTSETTELSE)),
    UTSETTELSE_GYLDIG_PGA_INNLEGGELSE_KUN_FAR_HAR_RETT("2018", "§14-11 første ledd bokstav c, jf. §14-14, jf. §14-13: Utsettelse pga. egen innleggelse på helseinstiusjon, kun far har rett", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,14-14,14-13\"}}}", Set.of(UTSETTELSE)),
    UTSETTELSE_GYLDIG_PGA_BARN_INNLAGT_KUN_FAR_HAR_RETT("2019", "§14-11 første ledd bokstav d, jf. §14-14, jf. §14-13: Utsettelse pga. barnets innleggelse på helseinstitusjon, kun far har rett", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,14-14,14-13\"}}}", Set.of(UTSETTELSE)),
    OVERFØRING_ANNEN_PART_HAR_IKKE_RETT_TIL_FORELDREPENGER("2020", "§14-9 første ledd: Overføring oppfylt, annen part har ikke rett til foreldrepenger", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-12\"}}}", Set.of(UTTAK), Set.of(MØDREKVOTE, FEDREKVOTE)),
    OVERFØRING_ANNEN_PART_SYKDOM_SKADE("2021", "§14-12: Overføring oppfylt, annen part er helt avhengig av hjelp til å ta seg av barnet", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-12\"}}}", Set.of(UTTAK), Set.of(MØDREKVOTE, FEDREKVOTE)),
    OVERFØRING_ANNEN_PART_INNLAGT("2022", "§14-12: Overføring oppfylt, annen part er innlagt i helseinstitusjon", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-12\"}}}", Set.of(UTTAK), Set.of(MØDREKVOTE, FEDREKVOTE)),
    OVERFØRING_SØKER_HAR_ALENEOMSORG_FOR_BARNET("2023", "§14-15 første ledd: Overføring oppfylt, søker har aleneomsorg for barnet", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-15\"}}}", Set.of(UTTAK), Set.of(MØDREKVOTE, FEDREKVOTE)),
    GRADERING_FELLESPERIODE_ELLER_FORELDREPENGER("2030", "§14-9, jf. §14-16: Gradering av fellesperiode/foreldrepenger", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-9,14-16\"}}}", Set.of(UTTAK), Set.of(FELLESPERIODE, FORELDREPENGER)),
    GRADERING_KVOTE_ELLER_OVERFØRT_KVOTE("2031", "§14-12, jf. §14-16: Gradering av kvote/overført kvote", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-12,14-16\"}}}", Set.of(UTTAK), Set.of(MØDREKVOTE, FEDREKVOTE)),
    GRADERING_ALENEOMSORG("2032", "§14-15, jf. §14-16: Gradering foreldrepenger ved aleneomsorg", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-15,14-16\"}}}", Set.of(UTTAK), Set.of(FORELDREPENGER)),
    GRADERING_FORELDREPENGER_KUN_FAR_HAR_RETT("2033", "§14-14, jf. §14-13, jf. §14-16: Gradering foreldrepenger, kun far har rett", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-14,14-13,14-16\"}}}", Set.of(UTTAK), Set.of(FORELDREPENGER)),
    GRADERING_FORELDREPENGER_KUN_MOR_HAR_RETT("2034", "§14-10, jf. §14-16: Gradering foreldrepenger, kun mor har rett", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10,14-16\"}}}", Set.of(UTTAK), Set.of(FORELDREPENGER)),
    FORELDREPENGER_KUN_FAR_HAR_RETT_MOR_UFØR("2036", "§14-14 tredje ledd: Innvilget foreldrepenger, kun far har rett og mor er ufør", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-14\"}}}", Set.of(UTTAK), Set.of(FORELDREPENGER)),
    FORELDREPENGER_FELLESPERIODE_TIL_FAR("2037", "§14-9, jf. §14-13: Innvilget fellesperiode til far", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-9\"}}}", Set.of(UTTAK), Set.of(FELLESPERIODE)),
    FORELDREPENGER_REDUSERT_GRAD_PGA_SAMTIDIG_UTTAK("2038", "§ 14-10 sjette ledd: Samtidig uttak", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK)),
    ;

    public static PeriodeResultatÅrsak UKJENT = PeriodeResultatÅrsak.UKJENT;
    private static final Map<String, InnvilgetÅrsak> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "INNVILGET_AARSAK";

    @Deprecated
    public static final String DISCRIMINATOR = "INNVILGET_AARSAK";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonIgnore
    private String navn;

    @JsonIgnore
    private String lovHjemmel;

    @JsonIgnore
    private LocalDate gyldigFom;

    @JsonIgnore
    private LocalDate gyldigTom;

    private String kode;

    private Set<UttakType> uttakTyper;

    private Set<StønadskontoType> valgbarForKonto;

    InnvilgetÅrsak(String kode,
                     String navn,
                     String lovHjemmel,
                     Set<UttakType> uttakTyper,
                     Set<StønadskontoType> valgbarForKonto,
                     LocalDate gyldigFom,
                     LocalDate gyldigTom) {
        this.kode = kode;
        this.navn = navn;
        this.lovHjemmel = lovHjemmel;
        this.gyldigFom = gyldigFom == null ? LocalDate.of(2000, 1, 1) : gyldigFom;
        this.gyldigTom = gyldigTom == null ? Tid.TIDENES_ENDE : gyldigTom;
        this.uttakTyper = uttakTyper == null ? Set.of(UTTAK) : uttakTyper;
        this.valgbarForKonto = valgbarForKonto == null ?
            Set.of(FELLESPERIODE, MØDREKVOTE, FEDREKVOTE, FORELDREPENGER, FORELDREPENGER_FØR_FØDSEL)
            : valgbarForKonto;
    }

    InnvilgetÅrsak(String kode, String navn, String lovHjemmel, Set<UttakType> uttakTyper, Set<StønadskontoType> valgbarForKonto) {
        this(kode, navn, lovHjemmel, uttakTyper, valgbarForKonto, null, null);
    }

    InnvilgetÅrsak(String kode, String navn, String lovHjemmel, Set<UttakType> uttakTyper) {
        this(kode, navn, lovHjemmel, uttakTyper, null, null, null);
    }

    InnvilgetÅrsak(String kode, String navn, String lovHjemmel, Set<UttakType> uttakTyper, LocalDate gyldigTom) {
        this(kode, navn, lovHjemmel, uttakTyper, null, null, gyldigTom);
    }

    @Override
    public LocalDate getGyldigFraOgMed() {
        return gyldigFom;
    }

    @Override
    public LocalDate getGyldigTilOgMed() {
        return gyldigTom;
    }

    @Override
    public Set<UttakType> getUttakTyper() {
        return uttakTyper;
    }

    @Override
    public Set<StønadskontoType> getValgbarForKonto() {
        return valgbarForKonto;
    }

    @JsonCreator
    public static InnvilgetÅrsak fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent InnvilgetÅrsak: " + kode);
        }
        return ad;
    }

    public static Map<String, InnvilgetÅrsak> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    /** Returnerer p.t. Raw json. */
    @Override
    public String getLovHjemmelData() {
        return lovHjemmel;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<InnvilgetÅrsak, String> {
        @Override
        public String convertToDatabaseColumn(InnvilgetÅrsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public InnvilgetÅrsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

    public static class MyInnvilgetÅrsakPeriodeResultatÅrsakSerializer extends PeriodeResultatÅrsakSerializer<InnvilgetÅrsak> {
        public MyInnvilgetÅrsakPeriodeResultatÅrsakSerializer() {
            super(InnvilgetÅrsak.class);
        }
    }
}
