package no.nav.foreldrepenger.datavarehus.v2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.datavarehus.domene.VilkårIkkeOppfylt;

public class StønadsstatistikkVedtak {
    // Teknisk tid
    @Valid
    private Saksnummer saksnummer; // felt 1
    private Long fagsakId;
    @NotNull
    private YtelseType ytelseType; // felt 39
    @NotNull
    private LovVersjon lovVersjon;
    @NotNull
    private UUID behandlingUuid;
    private UUID forrigeBehandlingUuid;
    private LocalDate skjæringstidspunkt;
    @NotNull
    private LocalDateTime vedtakstidspunkt; // Funksjonelt tid
    @NotNull
    private VedtakResultat vedtaksresultat;
    private VilkårIkkeOppfylt vilkårIkkeOppfylt; //kun opphør og avslag
    @NotNull
    @Valid
    private AktørId søker;
    @NotNull
    private Saksrolle søkersRolle;
    @NotNull
    private UtlandsTilsnitt utlandsTilsnitt;
    @Valid
    private AnnenForelder annenForelder;
    @NotNull
    @Valid
    private FamilieHendelse familieHendelse;
    private Beregning beregning;
    @NotNull
    private String utbetalingsreferanse; // en referanse mot oppdrag
    //ES
    private Long engangsstønadInnvilget;

    @Valid
    private ForeldrepengerRettigheter foreldrepengerRettigheter; //konto saldo, utregnet ut i fra rettigheter, minsteretter
    private List<@Valid StønadsstatistikkUttakPeriode> uttaksperioder;
    private List<@Valid StønadsstatistikkUtbetalingPeriode> utbetalingssperioder;


    // Etter møte: Dokumentasjonsperiode for aleneomsorg per uttaksperioder
    // Etter møte: annen forelder har engangsstønad
    // Etter møte: Mann tar foreldrepenger - MORS_AKTIVITET er null i ca 12%
    // Viktig å kunne agreggere trekkdager

    // Yrkeskoder ligger på arbeidsforhold - skal vi sende arbeidsforhold-id slikt at man kan hente det inn AREG - hva om areg slutter med arbeforhID

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    public YtelseType getYtelseType() {
        return ytelseType;
    }

    public LovVersjon getLovVersjon() {
        return lovVersjon;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public UUID getForrigeBehandlingUuid() {
        return forrigeBehandlingUuid;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public LocalDateTime getVedtakstidspunkt() {
        return vedtakstidspunkt;
    }

    public VedtakResultat getVedtaksresultat() {
        return vedtaksresultat;
    }

    public VilkårIkkeOppfylt getVilkårIkkeOppfylt() {
        return vilkårIkkeOppfylt;
    }

    public AktørId getSøker() {
        return søker;
    }

    public Saksrolle getSøkersRolle() {
        return søkersRolle;
    }

    public UtlandsTilsnitt getUtlandsTilsnitt() {
        return utlandsTilsnitt;
    }

    public AnnenForelder getAnnenForelder() {
        return annenForelder;
    }

    public FamilieHendelse getFamilieHendelse() {
        return familieHendelse;
    }

    public Beregning getBeregning() {
        return beregning;
    }

    public String getUtbetalingsreferanse() {
        return utbetalingsreferanse;
    }

    public Long getEngangsstønadInnvilget() {
        return engangsstønadInnvilget;
    }

    public List<StønadsstatistikkUttakPeriode> getUttaksperioder() {
        return uttaksperioder;
    }

    public List<StønadsstatistikkUtbetalingPeriode> getUtbetalingssperioder() {
        return utbetalingssperioder;
    }

    public ForeldrepengerRettigheter getForeldrepengerRettigheter() {
        return foreldrepengerRettigheter;
    }

    record Beregning(@NotNull BigDecimal grunnbeløp, @NotNull BeregningÅrsbeløp årsbeløp, Set<String> næringOrgNr) { } //på skjæringstidspunkt

    record FamilieHendelse(LocalDate termindato,
                           LocalDate adopsjonsdato,
                           @NotNull @Positive Integer antallBarn,
                           @NotEmpty @Valid List<Barn> barn, // AktørId setter ikke ved adopsjon og utenlandsfødte barn
                           @NotNull HendelseType hendelseType) {

        record Barn(AktørId aktørId, @NotNull LocalDate fødselsdato, LocalDate dødsdato) {}


    }

    enum HendelseType {
        FØDSEL, ADOPSJON, OMSORGSOVERTAKELSE
    }

    enum StønadskontoType {
        FORELDREPENGER,
        FORELDREPENGER_FØR_FØDSEL,
        MØDREKVOTE,
        FELLESPERIODE,
        FEDREKVOTE
    }

    enum Saksrolle {
        MOR, FAR, MEDMOR, UKJENT
    }

    enum Dekningsgrad {
        ÅTTI, HUNDRE
    }

    enum RettighetType {
        ALENEOMSORG, BARE_SØKER_RETT, BEGGE_RETT, BEGGE_RETT_EØS
    }

    record ForeldrepengerRettigheter(@NotNull @Valid Dekningsgrad dekningsgrad,
                                     @NotNull RettighetType rettighetType,
                                     @NotNull @NotEmpty @Valid Set<Stønadskonto> stønadskonti,
                                     @Valid Trekkdager flerbarnsdager) {

        record Stønadskonto(@NotNull StønadskontoType type,
                            @NotNull @Valid Trekkdager maksdager,
                            @NotNull @Valid Trekkdager restdager,
                            @Valid Trekkdager minsterett) {
        }

        // minsterett - kun for far har rett, uføre (mors aktivitet er ikke et krav i disse tilfeller)

        record Trekkdager(@JsonValue @Min(0) @Max(500) @NotNull BigDecimal antall) {
            public Trekkdager(int antall) {
                this(BigDecimal.valueOf(antall).setScale(1, RoundingMode.DOWN));
            }
        }
    }

    record AnnenForelder(@NotNull @Valid AktørId aktørId, @Valid Saksnummer saksnummer, YtelseType ytelseType, Saksrolle saksrolle) {}

    public record AktørId(@NotNull @Pattern(regexp = VALID_REGEXP, message = "AktørId ${validatedValue} har ikke gyldig verdi (pattern '{regexp}')")
                          @JsonValue String id) {
        private static final String VALID_REGEXP = "^\\d{13}$";
    }

    record Saksnummer(@NotNull @Pattern(regexp = VALID_REGEXP, message = "Saksnummer ${validatedValue} har ikke gyldig verdi (pattern '{regexp}')")
                      @JsonValue String id) {
        private static final String VALID_REGEXP = "^[0-9]*$";
    }

    enum LovVersjon {
        FORELDREPENGER_2019_01_01, // LOV-2017-12-19-116 - 1/1-2019
        FORELDREPENGER_FRI_2021_10_01, // LOV-2021-06-11-61 - 1/10-2021 - fri utsettelse
        FORELDREPENGER_MINSTERETT_2022_08_02, // LOV-2022-03-18-11 - 2/8-2022 - minsterett

        ENGANGSSTØNAD_2019_01_01,
        SVANGERSKAPSPENGER_2019_01_01,
    }

    enum UtlandsTilsnitt {
        NASJONAL, EØS_BOSATT_NORGE, BOSATT_UTLAND
    }

    enum VedtakResultat {
        INNVILGET, AVSLAG, OPPHØR
    }

    enum YtelseType {
        FORELDREPENGER, SVANGERSKAPSPENGER, ENGANGSSTØNAD
    }

    static class Builder {

        private final StønadsstatistikkVedtak kladd = new StønadsstatistikkVedtak();

        Builder medSak(Saksnummer saksnummer, Long fagsakId) {
            kladd.saksnummer = saksnummer;
            kladd.fagsakId = fagsakId;
            return this;
        }
        Builder medYtelseType(YtelseType ytelseType) {
            kladd.ytelseType = ytelseType;
            return this;
        }
        Builder medLovVersjon(LovVersjon lovVersjon) {
            kladd.lovVersjon = lovVersjon;
            return this;
        }
        Builder medBehandlingUuid(UUID behandlingUuid) {
            kladd.behandlingUuid = behandlingUuid;
            return this;
        }
        Builder medForrigeBehandlingUuid(UUID forrigeBehandlingUuid) {
            kladd.forrigeBehandlingUuid = forrigeBehandlingUuid;
            return this;
        }
        Builder medSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
            kladd.skjæringstidspunkt = skjæringstidspunkt;
            return this;
        }
        Builder medVedtakstidspunkt(LocalDateTime vedtakstidspunkt) {
            kladd.vedtakstidspunkt = vedtakstidspunkt;
            return this;
        }
        Builder medVedtaksresultat(VedtakResultat vedtaksresultat) {
            kladd.vedtaksresultat = vedtaksresultat;
            return this;
        }
        Builder medVilkårIkkeOppfylt(VilkårIkkeOppfylt vilkårIkkeOppfylt) {
            kladd.vilkårIkkeOppfylt = vilkårIkkeOppfylt;
            return this;
        }
        Builder medSøker(AktørId søker) {
            kladd.søker = søker;
            return this;
        }
        Builder medSøkersRolle(Saksrolle saksrolle) {
            kladd.søkersRolle = saksrolle;
            return this;
        }
        Builder medUtlandsTilsnitt(UtlandsTilsnitt utlandsTilsnitt) {
            kladd.utlandsTilsnitt = utlandsTilsnitt;
            return this;
        }
        Builder medAnnenForelder(AnnenForelder annenForelder) {
            kladd.annenForelder = annenForelder;
            return this;
        }
        Builder medFamilieHendelse(FamilieHendelse familieHendelse) {
            kladd.familieHendelse = familieHendelse;
            return this;
        }
        Builder medBeregning(Beregning beregning) {
            kladd.beregning = beregning;
            return this;
        }
        Builder medUtbetalingsreferanse(String utbetalingsreferanse) {
            kladd.utbetalingsreferanse = utbetalingsreferanse;
            return this;
        }
        Builder medEngangsstønadInnvilget(Long engangsstønadInnvilget) {
            kladd.engangsstønadInnvilget = engangsstønadInnvilget;
            return this;
        }

        Builder medForeldrepengerRettigheter(ForeldrepengerRettigheter foreldrepengerRettigheter) {
            kladd.foreldrepengerRettigheter = foreldrepengerRettigheter;
            return this;
        }

        Builder medUtbetalingssperioder(List<StønadsstatistikkUtbetalingPeriode> utbetalingssperioder) {
            kladd.utbetalingssperioder = utbetalingssperioder;
            return this;
        }
        Builder medUttakssperioder(List<StønadsstatistikkUttakPeriode> uttaksperioder) {
            kladd.uttaksperioder = uttaksperioder;
            return this;
        }
        public StønadsstatistikkVedtak build() {
            return kladd;
        }
    }

    record BeregningÅrsbeløp(BigDecimal brutto, BigDecimal avkortet, BigDecimal redusert) {
    }
}
