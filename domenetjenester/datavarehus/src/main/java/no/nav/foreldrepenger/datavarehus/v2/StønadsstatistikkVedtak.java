package no.nav.foreldrepenger.datavarehus.v2;

import java.math.BigDecimal;
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
import jakarta.validation.constraints.Positive;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.datavarehus.domene.VilkårVerdiDvh;

public class StønadsstatistikkVedtak {
    // Teknisk tid
    @Valid
    private Saksnummer saksnummer; // felt 1
    @NotNull
    private YtelseType ytelseType; // felt 39
    @NotNull
    private LovVersjon lovVersjon;
    @NotNull
    private UUID behandlingUuid;
    private UUID forrigeBehandlingUuid;
    private LocalDateTime skjæringstidspunkt;
    @NotNull
    private LocalDateTime vedtakstidspunkt; // Funksjonelt tid
    @NotNull
    private VedtakResultat vedtaksresultat;
    private VilkårVerdiDvh vilkårIkkeOppfylt; //kun opphør og avslag
    @NotNull
    @Valid
    private AktørId søker;
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

    // Blir ikke avkortet - brutto inntekt per år
    private BigDecimal bruttoÅrsinntekt;
    //ES
    private BigDecimal engangsstønadInnvilget;

    //SVP på periode???
    private LocalDate tilretteleggingsbehovFom;

    private List<StønadsstatistikkUttakPeriode> uttaksperioder;
    private List<StønadsstatistikkUtbetalingPeriode> utbetalingssperioder;
    private ForeldrepengerRettigheter foreldrepengerRettigheter; //konto saldo, utregnet ut i fra rettigheter, minsteretter

    // Nei: Inntektsmeldingen er dette interesant? Se på personopplysninger-dvh-fp-v2.xsd - svar fra Hans - ikke interessant
    // Nei: Verge, familiehendelse, inntektsmeldinger - trenges ikke
    // Etter møte: Dokumentasjonsperiode for aleneomsorg per uttaksperioder
    // Etter møte: annen forelder har engangsstønad
    // Etter møte: Mann tar foreldrepenger - MORS_AKTIVITET er null i ca 12%
    // Viktig å kunne agreggere trekkdager

    // Yrkeskoder ligger på arbeidsforhold - skal vi sende arbeidsforhold-id slikt at man kan hente det inn AREG - hva om areg slutter med arbeforhID


    StønadsstatistikkVedtak(Saksnummer saksnummer,
                            YtelseType ytelseType,
                            UUID behandlingUuid,
                            AktørId søker,
                            ForeldrepengerRettigheter foreldrepengerRettigheter) {
        this.saksnummer = saksnummer;
        this.ytelseType = ytelseType;
        this.behandlingUuid = behandlingUuid;
        this.søker = søker;
        this.foreldrepengerRettigheter = foreldrepengerRettigheter;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
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

    public LocalDateTime getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public LocalDateTime getVedtakstidspunkt() {
        return vedtakstidspunkt;
    }

    public VedtakResultat getVedtaksresultat() {
        return vedtaksresultat;
    }

    public VilkårVerdiDvh getVilkårIkkeOppfylt() {
        return vilkårIkkeOppfylt;
    }

    public AktørId getSøker() {
        return søker;
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

    public BigDecimal getBruttoÅrsinntekt() {
        return bruttoÅrsinntekt;
    }

    public BigDecimal getEngangsstønadInnvilget() {
        return engangsstønadInnvilget;
    }

    public LocalDate getTilretteleggingsbehovFom() {
        return tilretteleggingsbehovFom;
    }

    public List<StønadsstatistikkUtbetalingPeriode> getVedtaksperioder() {
        return utbetalingssperioder;
    }

    public ForeldrepengerRettigheter getForeldrepengerRettigheter() {
        return foreldrepengerRettigheter;
    }

    record Beregning(@NotNull BigDecimal bruttoÅrsinntekt, Set<AktivitetStatus> aktivitetStatuser, Set<String> næringOrgNr) {} //på skjæringstidspunkt

    record FamilieHendelse(@NotNull LocalDate termindato,
                           LocalDate adopsjonsdato,
                           @NotNull @Positive Integer antallBarn,
                           @NotEmpty @Valid List<Barn> barn, // AktørId setter ikke ved adopsjon og utenlandsfødte barn
                           @NotNull HendelseType behandlingTema) {

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

    enum Dekningsgrad {
        ÅTTI, HUNDRE
    }

    enum RettighetType {
        ALENEOMSORG, BARE_SØKER_RETT, BEGGE_RETT, BEGGE_RETT_EØS
    }

    record ForeldrepengerRettigheter(@NotNull @Valid Dekningsgrad dekningsgrad,
    @NotNull RettighetType rettighetType,
    @NotNull @NotEmpty @Valid Set<Stønadskonto> stønadskonti,
    @Valid Trekkdager flerbarnsdager,
    @Valid Trekkdager prematurdager,
    @Valid Trekkdager minsterett) { // kun ved to tette

        record Stønadskonto(@NotNull StønadskontoType type, @NotNull @Valid Trekkdager maksdager, @Valid Trekkdager minsterett) {}

        // minsterett - kun for far har rett, uføre (mors aktivitet er ikke et krav i disse tilfeller)

        record Trekkdager(@Min(0) @Max(500) @NotNull BigDecimal antall) {
            public static final Trekkdager ZERO = new Trekkdager(0);

            public Trekkdager(int antall) {
                this(BigDecimal.valueOf(antall));
            }
        }

    }

    record AnnenForelder(@NotNull @Valid AktørId aktørId, Saksnummer saksnummer) {}

    public record AktørId(@NotNull @jakarta.validation.constraints.Pattern(regexp = VALID_REGEXP, message = "AktørId ${validatedValue} har ikke gyldig verdi (pattern '{regexp}')")
                          String id) {
        private static final String VALID_REGEXP = "^\\d{13}$";
    }

    record Saksnummer(@NotNull @jakarta.validation.constraints.Pattern(regexp = VALID_REGEXP, message = "Saksnummer ${validatedValue} har ikke gyldig verdi (pattern '{regexp}')")
                      String id) {
        private static final String VALID_REGEXP = "^[0-9]*$";
    }

    enum LovVersjon {
        FØRSTE_FPSAK, // LOV-2017-12-19-116 - 1/1-2019
        PREMATURDAGER, // LOV-2019-06-21-28 - 1/7-2019 - prematurdager
        FRI_UTSETTELSE, // LOV-2021-06-11-61 - 1/10-2021 - fri utsettelse
        MINSTERETT_22 // LOV-2022-03-18-11 - 2/8-2022 - minsterett
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
}
