package no.nav.foreldrepenger.domene.prosess;

import static java.util.Collections.singletonList;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittFrilans;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelse;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@Dependent // La stå @Dependent så lenge #oppgittOpptjeningBuilder er et felt her
public class BeregningIAYTestUtil {

    public static final AktørId AKTØR_ID = AktørId.dummy();

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private OppgittOpptjeningBuilder oppgittOpptjeningBuilder; // FIXME: bør ikke ha state avh. felter i en CDI bønne

    BeregningIAYTestUtil() {
        // for CDI
    }

    @Inject
    public BeregningIAYTestUtil(InntektArbeidYtelseTjeneste iayTjeneste) {
        this.oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        this.iayTjeneste = iayTjeneste;
    }

    public InntektArbeidYtelseTjeneste getIayTjeneste() {
        return iayTjeneste;
    }

    /**
     * Lager oppgitt opptjening for Selvstending næringsdrivende 6 måneder før skjæringstidspunkt med endringsdato en måned før
     * skjæringstidspunkt.
     *
     * @param behandlingReferanse          aktuell behandling
     * @param skjæringstidspunktOpptjening skjæringstidpunkt for opptjening
     * @param nyIArbeidslivet              spesifiserer om bruker er ny i arbeidslivet
     * @param virksomhetType               spesifiserer virksomhetstype for næringsvirksomheten
     * @param perioder                     spesifiserer perioder
     */
    public void lagOppgittOpptjeningForSN(BehandlingReferanse behandlingReferanse,
                                          LocalDate skjæringstidspunktOpptjening,
                                          boolean nyIArbeidslivet,
                                          VirksomhetType virksomhetType,
                                          Collection<Periode> perioder) {
        List<OppgittOpptjeningBuilder.EgenNæringBuilder> næringBuilders = new ArrayList<>();
        perioder.forEach(periode -> {
            næringBuilders.add(OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
                .medBruttoInntekt(BigDecimal.valueOf(10000))
                .medNyIArbeidslivet(nyIArbeidslivet)
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom()))
                .medVirksomhetType(virksomhetType)
                .medEndringDato(skjæringstidspunktOpptjening.minusMonths(1)));
        });
        oppgittOpptjeningBuilder.leggTilEgneNæringer(næringBuilders);
        lagreOppgittOpptjening(behandlingReferanse);
    }

    /**
     * Lager oppgitt opptjening for Selvstending næringsdrivende 6 måneder før skjæringstidspunkt med endringsdato en måned før
     * skjæringstidspunkt.
     *
     * @param skjæringstidspunktOpptjening skjæringstidpunkt for opptjening
     * @param nyIArbeidslivet              spesifiserer om bruker er ny i arbeidslivet
     * @param virksomhetType               spesifiserer virksomhetstype for næringsvirksomheten
     * @param perioder                     spesifiserer perioder
     */
    public void byggPåOppgittOpptjeningForSN(LocalDate skjæringstidspunktOpptjening,
                                             boolean nyIArbeidslivet,
                                             VirksomhetType virksomhetType,
                                             Collection<Periode> perioder) {
        List<OppgittOpptjeningBuilder.EgenNæringBuilder> næringBuilders = new ArrayList<>();
        perioder.forEach(periode -> {
            næringBuilders.add(OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
                .medBruttoInntekt(BigDecimal.valueOf(10000))
                .medNyIArbeidslivet(nyIArbeidslivet)
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom()))
                .medVirksomhetType(virksomhetType)
                .medEndringDato(skjæringstidspunktOpptjening.minusMonths(1)));
        });
        oppgittOpptjeningBuilder.leggTilEgneNæringer(næringBuilders);
    }

    /**
     * Lager oppgitt opptjening for annen aktivitet som f.eks militærtjeneste, vartpenger, ventelønn m.m.
     *
     * @param arbeidType arbeidType for aktivitet
     * @param fom        fra dato
     * @param tom        til dato
     */
    public OppgittOpptjening byggPåOppgittOpptjeningAnnenAktivitet(ArbeidType arbeidType,
                                                                   LocalDate fom,
                                                                   LocalDate tom) {
        var annenAktivitet = new OppgittAnnenAktivitet(
            tom == null ? DatoIntervallEntitet.fraOgMed(fom) : DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom),
            arbeidType);
        oppgittOpptjeningBuilder.leggTilAnnenAktivitet(annenAktivitet);
        return oppgittOpptjeningBuilder.build();
    }

    private void lagFrilans(boolean erNyOppstartet, Collection<Periode> perioder) {
        perioder.forEach(periode -> oppgittOpptjeningBuilder.leggTilAnnenAktivitet(mapFrilansPeriode(periode)));
        var frilans = new OppgittFrilans();
        frilans.setErNyoppstartet(erNyOppstartet);
        oppgittOpptjeningBuilder.leggTilFrilansOpplysninger(frilans);
    }

    /**
     * Lager oppgitt opptjening for frilans med periode
     *
     * @param erNyOppstartet spesifiserer om frilans er nyoppstartet
     * @param perioder       perioder med aktiv frilans oppgitt i søknaden
     */
    public void byggPåOppgittOpptjeningForFL(boolean erNyOppstartet, Collection<Periode> perioder) {
        lagFrilans(erNyOppstartet, perioder);
    }

    private OppgittAnnenAktivitet mapFrilansPeriode(Periode periode) {
        var datoIntervallEntitet = mapPeriode(periode);
        return new OppgittAnnenAktivitet(datoIntervallEntitet, ArbeidType.FRILANSER);
    }

    private DatoIntervallEntitet mapPeriode(Periode periode) {
        var fom = periode.getFom();
        var tom = periode.getTom();
        if (tom == null) {
            return DatoIntervallEntitet.fraOgMed(fom);
        }
        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    public void lagreOppgittOpptjening(BehandlingReferanse behandlingReferanse) {
        iayTjeneste.lagreOppgittOpptjening(behandlingReferanse.behandlingId(), oppgittOpptjeningBuilder);
        oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
    }

    public void leggTilAktørytelse(BehandlingReferanse behandlingReferanse,
                                   LocalDate fom,
                                   LocalDate tom,
                                   RelatertYtelseTilstand relatertYtelseTilstand,
                                   String saksnummer,
                                   RelatertYtelseType ytelseType,
                                   List<YtelseStørrelse> ytelseStørrelseList,
                                   Arbeidskategori arbeidskategori,
                                   Periode... meldekortPerioder) {
        var inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(
            behandlingReferanse.behandlingId());
        var aktørYtelseBuilder = inntektArbeidYtelseAggregatBuilder.getAktørYtelseBuilder(
            behandlingReferanse.aktørId());
        var ytelseBuilder = aktørYtelseBuilder.getYtelselseBuilderForType(Fagsystem.INFOTRYGD, ytelseType,
            Saksnummer.infotrygd(saksnummer));
        ytelseBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        ytelseBuilder.medStatus(relatertYtelseTilstand);
        var ytelseGrunnlagBuilder = ytelseBuilder.getGrunnlagBuilder()
            .medArbeidskategori(arbeidskategori);
        ytelseStørrelseList.forEach(ytelseGrunnlagBuilder::medYtelseStørrelse);
        ytelseBuilder.medYtelseGrunnlag(ytelseGrunnlagBuilder.build());
        if (meldekortPerioder != null) {
            Arrays.asList(meldekortPerioder).forEach(meldekortPeriode -> {
                var ytelseAnvist = lagYtelseAnvist(meldekortPeriode, ytelseBuilder);
                ytelseBuilder.medYtelseAnvist(ytelseAnvist);
            });
        }
        aktørYtelseBuilder.leggTilYtelse(ytelseBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørYtelse(aktørYtelseBuilder);
        iayTjeneste.lagreIayAggregat(behandlingReferanse.behandlingId(), inntektArbeidYtelseAggregatBuilder);
    }

    private YtelseAnvist lagYtelseAnvist(Periode periode, YtelseBuilder ytelseBuilder) {
        return ytelseBuilder.getAnvistBuilder()
            .medAnvistPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom()))
            .medUtbetalingsgradProsent(BigDecimal.valueOf(100))
            .medDagsats(BigDecimal.valueOf(1000))
            .medBeløp(BigDecimal.valueOf(10000))
            .build();
    }

    public InntektArbeidYtelseAggregat byggArbeidForBehandling(BehandlingReferanse behandlingReferanse,
                                                               LocalDate skjæringstidspunktOpptjening,
                                                               LocalDate fraOgMed,
                                                               LocalDate tilOgMed,
                                                               InternArbeidsforholdRef arbId,
                                                               Arbeidsgiver arbeidsgiver) {
        return byggArbeidForBehandling(behandlingReferanse, skjæringstidspunktOpptjening, fraOgMed, tilOgMed, arbId,
            arbeidsgiver, BigDecimal.TEN);
    }

    public InntektArbeidYtelseAggregat byggArbeidForBehandling(BehandlingReferanse behandlingReferanse,
                                                               LocalDate skjæringstidspunktOpptjening,
                                                               LocalDate fraOgMed,
                                                               LocalDate tilOgMed,
                                                               InternArbeidsforholdRef arbId,
                                                               Arbeidsgiver arbeidsgiver,
                                                               BigDecimal inntektPrMnd) {
        return byggArbeidForBehandling(behandlingReferanse.behandlingId(), behandlingReferanse.aktørId(),
            skjæringstidspunktOpptjening, fraOgMed, tilOgMed, arbId, arbeidsgiver, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD,
            singletonList(inntektPrMnd), arbeidsgiver != null, false, Optional.empty());
    }

    private InntektArbeidYtelseAggregat byggArbeidForBehandling(Long behandlingId,
                                                                AktørId aktørId,
                                                                LocalDate skjæringstidspunktOpptjening,
                                                                LocalDate fraOgMed,
                                                                LocalDate tilOgMed,
                                                                InternArbeidsforholdRef arbId,
                                                                Arbeidsgiver arbeidsgiver,
                                                                ArbeidType arbeidType,
                                                                List<BigDecimal> inntektPrMnd,
                                                                boolean virksomhetPåInntekt,
                                                                boolean medPermisjon,
                                                                Optional<LocalDate> lønnsendringsdato) {
        InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder;
        inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandlingId);
        byggArbeidInntekt(behandlingId, aktørId, skjæringstidspunktOpptjening, fraOgMed, tilOgMed, arbId, arbeidsgiver,
            arbeidType, inntektPrMnd, virksomhetPåInntekt, medPermisjon, lønnsendringsdato,
            inntektArbeidYtelseAggregatBuilder);
        if (lønnsendringsdato.isPresent()) {
            brukUtenInntektsmelding(behandlingId, aktørId, arbeidType, arbeidsgiver, skjæringstidspunktOpptjening);
        }
        return inntektArbeidYtelseAggregatBuilder.build();
    }

    public void byggArbeidInntekt(Long behandlingId,
                                  AktørId aktørId,
                                  LocalDate skjæringstidspunktOpptjening,
                                  LocalDate fraOgMed,
                                  LocalDate tilOgMed,
                                  InternArbeidsforholdRef arbId,
                                  Arbeidsgiver arbeidsgiver,
                                  ArbeidType arbeidType,
                                  List<BigDecimal> inntektPrMnd,
                                  boolean virksomhetPåInntekt,
                                  boolean medPermisjon,
                                  Optional<LocalDate> lønnsendringsdato,
                                  InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder) {
        var aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder.getAktørArbeidBuilder(
            aktørId);
        var yrkesaktivitetBuilder = hentYABuilder(aktørArbeidBuilder, arbeidType, arbeidsgiver,
            arbId);

        var aktivitetsAvtale = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(
                tilOgMed == null ? DatoIntervallEntitet.fraOgMed(fraOgMed) : DatoIntervallEntitet.fraOgMedTilOgMed(
                    fraOgMed, tilOgMed))
            .medProsentsats(BigDecimal.TEN)
            .medSisteLønnsendringsdato(lønnsendringsdato.orElse(null));
        var arbeidsperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(
                tilOgMed == null ? DatoIntervallEntitet.fraOgMed(fraOgMed) : DatoIntervallEntitet.fraOgMedTilOgMed(
                    fraOgMed, tilOgMed));

        yrkesaktivitetBuilder.medArbeidType(arbeidType)
            .medArbeidsgiver(arbeidsgiver)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(arbeidsperiode);
        if (arbId != null) {
            yrkesaktivitetBuilder.medArbeidsforholdId(arbId);
        }

        if (medPermisjon) {
            leggTilPermisjon(fraOgMed, tilOgMed, PermisjonsbeskrivelseType.UTDANNINGSPERMISJON, yrkesaktivitetBuilder);
        }

        var aktørArbeid = aktørArbeidBuilder.leggTilYrkesaktivitet(
            yrkesaktivitetBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørArbeid(aktørArbeid);
        byggInntektForBehandling(iayTjeneste, behandlingId, aktørId, skjæringstidspunktOpptjening,
            inntektArbeidYtelseAggregatBuilder, inntektPrMnd, virksomhetPåInntekt, arbeidsgiver);
        iayTjeneste.lagreIayAggregat(behandlingId, inntektArbeidYtelseAggregatBuilder);
    }

    private void brukUtenInntektsmelding(Long behandlingId,
                                         AktørId aktørId,
                                         ArbeidType arbeidType,
                                         Arbeidsgiver arbeidsgiver,
                                         LocalDate skjæringstidspunktOpptjening) {
        var grunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(),
            grunnlag.getAktørArbeidFraRegister(aktørId)).før(skjæringstidspunktOpptjening);

        if (!filter.getYrkesaktiviteter().isEmpty()) {
            var yrkesaktivitet = finnKorresponderendeYrkesaktivitet(filter, arbeidType, arbeidsgiver);
            final var informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(
                iayTjeneste.finnGrunnlag(behandlingId));

            final var overstyringBuilderFor = informasjonBuilder.getOverstyringBuilderFor(
                yrkesaktivitet.getArbeidsgiver(), yrkesaktivitet.getArbeidsforholdRef());
            overstyringBuilderFor.medHandling(ArbeidsforholdHandlingType.BRUK_UTEN_INNTEKTSMELDING);
            informasjonBuilder.leggTil(overstyringBuilderFor);
            iayTjeneste.lagreArbeidsforhold(behandlingId, aktørId, informasjonBuilder);
        }
    }

    public static Yrkesaktivitet finnKorresponderendeYrkesaktivitet(YrkesaktivitetFilter filter,
                                                                    ArbeidType arbeidType,
                                                                    Arbeidsgiver arbeidsgiver) {
        var yrkesaktiviteter = finnKorresponderendeYrkesaktiviteter(filter, arbeidType);
        return yrkesaktiviteter.stream().filter(ya -> ya.getArbeidsgiver().equals(arbeidsgiver)).findFirst().get();
    }

    private static Collection<Yrkesaktivitet> finnKorresponderendeYrkesaktiviteter(YrkesaktivitetFilter filter,
                                                                                   ArbeidType arbeidType) {
        if (ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER.equals(arbeidType)) {
            return filter.getFrilansOppdrag();
        }
        return filter.getYrkesaktiviteter();
    }

    private static void leggTilPermisjon(LocalDate fraOgMed,
                                         LocalDate tilOgMed,
                                         PermisjonsbeskrivelseType permisjonsbeskrivelseType,
                                         YrkesaktivitetBuilder yrkesaktivitetBuilder) {
        var permisjon = yrkesaktivitetBuilder.getPermisjonBuilder()
            .medPeriode(fraOgMed, tilOgMed)
            .medPermisjonsbeskrivelseType(permisjonsbeskrivelseType)
            .medProsentsats(BigDecimal.valueOf(50))
            .build();
        yrkesaktivitetBuilder.leggTilPermisjon(permisjon);
    }

    private static YrkesaktivitetBuilder hentYABuilder(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder,
                                                       ArbeidType arbeidType,
                                                       Arbeidsgiver arbeidsgiver,
                                                       InternArbeidsforholdRef arbId) {
        if (arbId == null) {
            return aktørArbeidBuilder.getYrkesaktivitetBuilderForType(arbeidType);
        }
        return aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
            new Opptjeningsnøkkel(arbId, arbeidsgiver), arbeidType);

    }

    public static void byggInntektForBehandling(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                Long behandlingId,
                                                AktørId aktørId,
                                                LocalDate skjæringstidspunktOpptjening,
                                                InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder,
                                                List<BigDecimal> inntektPrMnd,
                                                boolean virksomhetPåInntekt,
                                                Arbeidsgiver arbeidsgiver) {

        var aktørInntekt = inntektArbeidYtelseAggregatBuilder.getAktørInntektBuilder(
            aktørId);

        var inntektBeregningBuilder = aktørInntekt.getInntektBuilder(InntektsKilde.INNTEKT_BEREGNING,
            Opptjeningsnøkkel.forArbeidsforholdIdMedArbeidgiver(null, arbeidsgiver));

        // Lager et år (12 mnd) med inntekt for beregning
        byggInntekt(inntektBeregningBuilder, skjæringstidspunktOpptjening, inntektPrMnd, virksomhetPåInntekt,
            arbeidsgiver);
        aktørInntekt.leggTilInntekt(inntektBeregningBuilder);

        var inntektSammenligningBuilder = aktørInntekt.getInntektBuilder(InntektsKilde.INNTEKT_SAMMENLIGNING,
            Opptjeningsnøkkel.forArbeidsforholdIdMedArbeidgiver(null, arbeidsgiver));

        // Lager et år (12 mnd) med inntekt for sammenligningsgrunnlag
        byggInntekt(inntektSammenligningBuilder, skjæringstidspunktOpptjening, inntektPrMnd, virksomhetPåInntekt,
            arbeidsgiver);
        aktørInntekt.leggTilInntekt(inntektSammenligningBuilder);

        inntektArbeidYtelseAggregatBuilder.leggTilAktørInntekt(aktørInntekt);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandlingId, inntektArbeidYtelseAggregatBuilder);
    }

    private static void byggInntekt(InntektBuilder builder,
                                    LocalDate skjæringstidspunktOpptjening,
                                    List<BigDecimal> inntektPrMnd,
                                    boolean virksomhetPåInntekt,
                                    Arbeidsgiver arbeidsgiver) {
        if (virksomhetPåInntekt) {
            for (var i = 0; i <= 12; i++) {
                var inntekt = getInntekt(inntektPrMnd, i);
                builder.leggTilInntektspost(
                    lagInntektspost(skjæringstidspunktOpptjening.minusMonths(i + 1L).plusDays(1),
                        skjæringstidspunktOpptjening.minusMonths(i), inntekt)).medArbeidsgiver(arbeidsgiver);
            }
        } else {
            for (var i = 0; i <= 12; i++) {
                var inntekt = getInntekt(inntektPrMnd, i);
                builder.leggTilInntektspost(
                    lagInntektspost(skjæringstidspunktOpptjening.minusMonths(i + 1L).plusDays(1),
                        skjæringstidspunktOpptjening.minusMonths(i), inntekt));
            }
        }
    }

    private static BigDecimal getInntekt(List<BigDecimal> inntektPrMnd, int i) {
        BigDecimal inntekt;
        if (inntektPrMnd.size() >= i + 1) {
            inntekt = inntektPrMnd.get(i);
        } else {
            inntekt = inntektPrMnd.get(inntektPrMnd.size() - 1);
        }
        return inntekt;
    }

    private static InntektspostBuilder lagInntektspost(LocalDate fom, LocalDate tom, BigDecimal lønn) {
        return InntektspostBuilder.ny().medBeløp(lønn).medPeriode(fom, tom).medInntektspostType(InntektspostType.LØNN);
    }
}
