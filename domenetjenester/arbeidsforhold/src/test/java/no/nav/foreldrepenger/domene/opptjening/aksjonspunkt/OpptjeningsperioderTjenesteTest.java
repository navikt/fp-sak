package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static no.nav.foreldrepenger.domene.arbeidsforhold.YtelseTestHelper.leggTilYtelseMedAnvist;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningsperioderTjeneste;
import no.nav.foreldrepenger.domene.opptjening.VurderingsStatus;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

@ExtendWith(JpaExtension.class)
class OpptjeningsperioderTjenesteTest {

    private static final String ORG_NUMMER = KUNSTIG_ORG;

    private BehandlingRepository behandlingRepository;

    private FagsakRepository fagsakRepository;

    private OpptjeningRepository opptjeningRepository;

    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final VirksomhetTjeneste virksomhetTjeneste = mock(VirksomhetTjeneste.class);
    private OpptjeningsperioderTjeneste forSaksbehandlingTjeneste;

    private final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.nyRef();
    private final AktørId AKTØRID = AktørId.dummy();
    private final LocalDate skjæringstidspunkt = LocalDate.now();

    @BeforeEach
    void setUp(EntityManager entityManager) {
        behandlingRepository = new BehandlingRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        opptjeningRepository = new OpptjeningRepository(entityManager, behandlingRepository);
        var aksjonspunktutlederForVurderOpptjening = new AksjonspunktutlederForVurderOppgittOpptjening(opptjeningRepository,
                iayTjeneste, virksomhetTjeneste);
        var apbOpptjening = new AksjonspunktutlederForVurderBekreftetOpptjening(opptjeningRepository, iayTjeneste);
        forSaksbehandlingTjeneste = new OpptjeningsperioderTjeneste(iayTjeneste, opptjeningRepository, aksjonspunktutlederForVurderOpptjening,
                apbOpptjening);
    }

    @Test
    void skal_utlede_opptjening_aktivitet_periode_uten_overstyrt() {
        // Arrange
        var behandling = opprettBehandling();

        var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt.minusMonths(2));
        var periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(2), skjæringstidspunkt.minusMonths(1));

        var oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode2, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        var virksomhet = Arbeidsgiver.virksomhet(ORG_NUMMER);
        var bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD,
            BigDecimal.TEN, virksomhet);
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        var saksbehandlet = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.of(bekreftet.build()),
                VersjonType.SAKSBEHANDLET);

        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandlet);

        // Act
        var behandlingRef = BehandlingReferanse.fra(behandling);
        var perioder = forSaksbehandlingTjeneste
                .hentRelevanteOpptjeningAktiveterForSaksbehandling(behandlingRef, medUtledetSkjæringstidspunkt(skjæringstidspunkt));

        // Assert
        assertThat(perioder).hasSize(2);
        var saksbehandletPeriode = perioder.stream().filter(p -> p.getOpptjeningsnøkkel()
                .getArbeidsforholdRef().map(r -> r.gjelderFor(ARBEIDSFORHOLD_ID)).orElse(false)).findFirst().get();
        assertThat(saksbehandletPeriode.getPeriode()).isEqualTo(periode1);
    }

    @Test
    void kun_register_varierende_stillingsprosent_innen_arbeidsforhold() {
        // Arrange
        final var behandling = opprettBehandling();

        var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusYears(1), skjæringstidspunkt.minusMonths(8).minusDays(1));
        var periode2 = DatoIntervallEntitet.fraOgMed(skjæringstidspunkt.minusMonths(8));

        final var navOrgnummer = "889640782";
        final var virksomhet = Arbeidsgiver.virksomhet(navOrgnummer);
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);

        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(AKTØRID);
        var yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
            new Opptjeningsnøkkel(ARBEIDSFORHOLD_ID, virksomhet.getIdentifikator(), null), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(virksomhet)
            .medArbeidsforholdId(ARBEIDSFORHOLD_ID);

        var aktivitetsAvtale1 = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder(periode1, false)
            .medProsentsats(BigDecimal.ZERO).medSisteLønnsendringsdato(periode1.getFomDato());
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(aktivitetsAvtale1);
        var aktivitetsAvtale2 = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder(periode2, false)
            .medProsentsats(BigDecimal.TEN).medSisteLønnsendringsdato(periode2.getFomDato());
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(aktivitetsAvtale2);
        var ansettelsesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMed(skjæringstidspunkt.minusYears(1)), true);
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(ansettelsesperiode);

        var aktørArbeid = aktørArbeidBuilder
            .leggTilYrkesaktivitet(yrkesaktivitetBuilder);

        builder.leggTilAktørArbeid(aktørArbeid);

        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusMonths(10), skjæringstidspunkt.minusDays(1), false);

        // Act
        var behandlingRef = BehandlingReferanse.fra(behandling);
        var perioder = forSaksbehandlingTjeneste
            .hentRelevanteOpptjeningAktiveterForVilkårVurdering(behandlingRef, medUtledetSkjæringstidspunkt(skjæringstidspunkt));

        // Assert
        assertThat(perioder).hasSize(2);
        assertThat(perioder.stream().anyMatch(p -> p.getVurderingsStatus().equals(VurderingsStatus.FERDIG_VURDERT_UNDERKJENT))).isTrue();
        assertThat(perioder.stream().anyMatch(p -> p.getVurderingsStatus().equals(VurderingsStatus.TIL_VURDERING))).isTrue();
    }

    @Test
    void ytelse_skal_lage_sammehengende_liste() {
        // Arrange
        var behandling = opprettBehandling();

        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt);

        var virksomhet = Arbeidsgiver.virksomhet(ORG_NUMMER);
        var bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD,
            BigDecimal.TEN, virksomhet);
        bekreftet.leggTilAktørYtelse(leggTilYtelseMedAnvist(bekreftet.getAktørYtelseBuilder(AKTØRID),
                VirkedagUtil.fomVirkedag(skjæringstidspunkt.minusDays(10)),
                VirkedagUtil.fredagLørdagTilSøndag(skjæringstidspunkt.minusDays(2)), RelatertYtelseTilstand.LØPENDE, "12342234", RelatertYtelseType.SYKEPENGER));
        bekreftet.leggTilAktørYtelse(leggTilYtelseMedAnvist(bekreftet.getAktørYtelseBuilder(AKTØRID),
                VirkedagUtil.fomVirkedag(skjæringstidspunkt.minusDays(25)),
                VirkedagUtil.fredagLørdagTilSøndag(skjæringstidspunkt.minusDays(5)), RelatertYtelseTilstand.AVSLUTTET, "1222433", RelatertYtelseType.SYKEPENGER));
        bekreftet.leggTilAktørYtelse(leggTilYtelseMedAnvist(bekreftet.getAktørYtelseBuilder(AKTØRID),
                VirkedagUtil.fomVirkedag(skjæringstidspunkt.minusDays(30)),
                VirkedagUtil.fredagLørdagTilSøndag(skjæringstidspunkt.minusDays(21)), RelatertYtelseTilstand.AVSLUTTET, "124234", RelatertYtelseType.SYKEPENGER));
        bekreftet.leggTilAktørYtelse(
                leggTilYtelseMedAnvist(bekreftet.getAktørYtelseBuilder(AKTØRID), VirkedagUtil.fomVirkedag(skjæringstidspunkt.minusDays(50)),
                        VirkedagUtil.fredagLørdagTilSøndag(skjæringstidspunkt.minusDays(40)), RelatertYtelseTilstand.AVSLUTTET, "123253254",
                        RelatertYtelseType.SYKEPENGER));

        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusDays(30), skjæringstidspunkt, false);

        // Act
        var behandlingRef = BehandlingReferanse.fra(behandling);
        var perioder = forSaksbehandlingTjeneste
                .hentRelevanteOpptjeningAktiveterForSaksbehandling(behandlingRef, medUtledetSkjæringstidspunkt(skjæringstidspunkt));

        // Assert
        assertThat(perioder).hasSize(3);
        var ytelser = perioder.stream()
                .filter(p -> OpptjeningAktivitetType.SYKEPENGER.equals(p.getOpptjeningAktivitetType()))
                .toList();
        assertThat(ytelser.get(0).getPeriode().getFomDato()).isEqualTo(VirkedagUtil.fomVirkedag(skjæringstidspunkt.minusDays(50)));
        assertThat(ytelser.get(1).getPeriode().getFomDato()).isEqualTo(VirkedagUtil.fomVirkedag(skjæringstidspunkt.minusDays(30)));
        assertThat(ytelser.get(1).getPeriode().getTomDato()).isEqualTo(VirkedagUtil.fredagLørdagTilSøndag(skjæringstidspunkt.minusDays(2)));

    }

    @Test
    void ytelse_skal_ikke_ta_med_ytelser_etter_stp() {
        // Arrange
        var behandling = opprettBehandling();

        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt);

        var virksomhet = Arbeidsgiver.virksomhet(ORG_NUMMER);
        var bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD,
            BigDecimal.TEN, virksomhet);
        bekreftet.leggTilAktørYtelse(leggTilYtelseMedAnvist(bekreftet.getAktørYtelseBuilder(AKTØRID),
                VirkedagUtil.fomVirkedag(skjæringstidspunkt.minusDays(15)),
                VirkedagUtil.fredagLørdagTilSøndag(skjæringstidspunkt.minusDays(2)), RelatertYtelseTilstand.LØPENDE, "12342234", RelatertYtelseType.SYKEPENGER));
        bekreftet.leggTilAktørYtelse(
                leggTilYtelseMedAnvist(bekreftet.getAktørYtelseBuilder(AKTØRID), VirkedagUtil.fomVirkedag(skjæringstidspunkt.minusDays(30)),
                        VirkedagUtil.fredagLørdagTilSøndag(skjæringstidspunkt.minusDays(15)), RelatertYtelseTilstand.AVSLUTTET, "1222433",
                        RelatertYtelseType.OMSORGSPENGER));
        bekreftet.leggTilAktørYtelse(leggTilYtelseMedAnvist(bekreftet.getAktørYtelseBuilder(AKTØRID),
                VirkedagUtil.fomVirkedag(skjæringstidspunkt.plusDays(10)),
                VirkedagUtil.fredagLørdagTilSøndag(skjæringstidspunkt.plusDays(21)), RelatertYtelseTilstand.AVSLUTTET, "124234", RelatertYtelseType.SYKEPENGER));

        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusDays(30), skjæringstidspunkt, false);

        // Act
        var behandlingRef = BehandlingReferanse.fra(behandling);
        var perioder = forSaksbehandlingTjeneste
                .hentRelevanteOpptjeningAktiveterForSaksbehandling(behandlingRef, medUtledetSkjæringstidspunkt(skjæringstidspunkt));

        // Assert
        assertThat(perioder).hasSize(3);
        var ytelserSP = perioder.stream()
                .filter(p -> OpptjeningAktivitetType.SYKEPENGER.equals(p.getOpptjeningAktivitetType()))
                .toList();
        assertThat(ytelserSP).hasSize(1);
        assertThat(ytelserSP.get(0).getPeriode().getFomDato()).isEqualTo(VirkedagUtil.fomVirkedag(skjæringstidspunkt.minusDays(15)));
        assertThat(perioder.stream().map(OpptjeningsperiodeForSaksbehandling::getOpptjeningAktivitetType)
                .filter(OpptjeningAktivitetType.OMSORGSPENGER::equals).count()).isEqualTo(1);

    }

    @Test
    void skal_sammenstille_grunnlag_og_overstyrt_deretter_utlede_opptjening_aktivitet_periode() {
        // Arrange
        var behandling = opprettBehandling();

        var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt.minusMonths(2));
        var periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(2), skjæringstidspunkt.minusMonths(1));
        var periode3 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(1), skjæringstidspunkt.minusMonths(0));

        var oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode2, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        var virksomhet = Arbeidsgiver.virksomhet(ORG_NUMMER);
        var bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD,
            BigDecimal.TEN, virksomhet);
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        var saksbehandlet = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.of(bekreftet.build()),
                VersjonType.SAKSBEHANDLET);
        var ref = saksbehandlet.medNyInternArbeidsforholdRef(virksomhet, EksternArbeidsforholdRef.ref("1"));

        var yrkesaktivitetBuilder = saksbehandlet.getAktørArbeidBuilder(AKTØRID)
                .getYrkesaktivitetBuilderForNøkkelAvType(new Opptjeningsnøkkel(ref, null, null),
                        ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktivitetBuilder
                .leggTilAktivitetsAvtale(yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periode3.getFomDato(), periode3.getTomDato()))
                        .medProsentsats(BigDecimal.TEN));

        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandlet);

        // Act
        var behandlingRef = BehandlingReferanse.fra(behandling);
        var perioder = forSaksbehandlingTjeneste
                .hentRelevanteOpptjeningAktiveterForSaksbehandling(behandlingRef, medUtledetSkjæringstidspunkt(skjæringstidspunkt));

        // Assert
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.GODKJENT)).toList()).hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.UNDERKJENT)).toList())
                .hasSize(1);
    }

    @Test
    void skal_sammenstille_grunnlag_og_utlede_opptjening_aktivitet_periode() {
        // Arrange
        var behandling = opprettBehandling();

        var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt.minusMonths(2));
        var periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(2), skjæringstidspunkt.minusMonths(1));

        var oppgitt = OppgittOpptjeningBuilder.ny();

        var egenNæringBuilder = OppgittOpptjeningBuilder.EgenNæringBuilder.ny();
        egenNæringBuilder
                .medRegnskapsførerNavn("Larsen")
                .medRegnskapsførerTlf("TELEFON")
                .medVirksomhet(ORG_NUMMER)
                .medPeriode(periode2);

        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode2, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        oppgitt.leggTilEgenNæring(List.of(egenNæringBuilder));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        var bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1,
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN, Arbeidsgiver.virksomhet(ORG_NUMMER));
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        var ref = BehandlingReferanse.fra(behandling);
        // Act
        var perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref, medUtledetSkjæringstidspunkt(skjæringstidspunkt));

        // Assert
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.GODKJENT)).toList()).hasSize(2);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.TIL_VURDERING)).toList())
                .hasSize(1);
        assertThat(perioder.stream().filter(OpptjeningsperiodeForSaksbehandling::getErManueltRegistrert).toList()).isEmpty();
        assertThat(perioder.stream().filter(o -> !o.getErManueltRegistrert()).toList()).hasSize(3);
    }

    @Test
    void skal_utlede_om_en_periode_er_blitt_endret() {
        // Arrange
        var behandling = opprettBehandling();

        var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt.minusMonths(2));
        var periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(2), skjæringstidspunkt.minusMonths(1));

        var oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode2, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        var saksbehandlet = opprettOverstyrtOppgittOpptjening(periode1,
                ArbeidType.MILITÆR_ELLER_SIVILTJENESTE, AKTØRID, VersjonType.SAKSBEHANDLET);
        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandlet);
        var ref = BehandlingReferanse.fra(behandling);
        // Act
        // Assert
        var perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref, medUtledetSkjæringstidspunkt(skjæringstidspunkt))
                .stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE))
                .toList();

        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getErPeriodeEndret()).isTrue();
        assertThat(perioder.get(0).getBegrunnelse()).isNotEmpty();
    }

    @Test
    void skal_returnere_oat_frilans_ved_bekreftet_frilans() {
        // Arrange
        var behandling = opprettBehandling();

        var arbeidsgiver = Arbeidsgiver.virksomhet(ORG_NUMMER);

        var fraOgMed = LocalDate.now().minusMonths(4);
        var tilOgMed = LocalDate.now().minusMonths(3);
        var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);

        var bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1,
                ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER, BigDecimal.TEN, arbeidsgiver);
        opprettInntektForFrilanser(bekreftet, AKTØRID, ARBEIDSFORHOLD_ID, periode1, arbeidsgiver);
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        var ref = BehandlingReferanse.fra(behandling);
        var stp = medUtledetSkjæringstidspunkt(skjæringstidspunkt);
        opptjeningRepository.lagreOpptjeningsperiode(behandling, LocalDate.now().minusMonths(10), LocalDate.now().minusDays(1), false);

        // Act 1
        var perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref, stp);
        var perioderVilkår = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(ref, stp);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).toList()).hasSize(1);
        assertThat(perioderVilkår.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILOPP)).toList()).hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.TIL_VURDERING)).toList()).hasSize(1);
        assertThat(perioderVilkår.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.TIL_VURDERING)).toList()).hasSize(1);
        assertThat(perioder.stream().filter(OpptjeningsperiodeForSaksbehandling::getErManueltRegistrert).toList()).isEmpty();
        assertThat(perioder.stream().filter(o -> !o.getErManueltRegistrert()).toList()).hasSize(1);
        assertThat(perioderVilkår.stream().filter(o -> !o.getErManueltRegistrert()).toList()).hasSize(1);

        // Act 2
        var saksbehandlet = opprettOverstyrtOppgittOpptjening(periode1,
                ArbeidType.MILITÆR_ELLER_SIVILTJENESTE, AKTØRID, VersjonType.SAKSBEHANDLET);
        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandlet);
        perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref, stp);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).toList()).hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE)).toList()).hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.GODKJENT)).toList()).hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.UNDERKJENT)).toList()).hasSize(1);
        assertThat(perioder.stream().filter(OpptjeningsperiodeForSaksbehandling::getErManueltRegistrert).toList()).hasSize(1);
    }

    @Test
    void skal_sette_manuelt_behandlet_ved_underkjent_frilans() {
        // Arrange
        var behandling = opprettBehandling();

        var arbeidsgiver = Arbeidsgiver.virksomhet(ORG_NUMMER);

        var fraOgMed = LocalDate.now().minusMonths(4);
        var tilOgMed = LocalDate.now().minusMonths(3);
        var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);

        var bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1,
                ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER, BigDecimal.TEN, arbeidsgiver);
        opprettInntektForFrilanser(bekreftet, AKTØRID, ARBEIDSFORHOLD_ID, periode1, arbeidsgiver);
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        var oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode1, ArbeidType.FRILANSER));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        var ref = BehandlingReferanse.fra(behandling);
        var stp = medUtledetSkjæringstidspunkt(skjæringstidspunkt);
        opptjeningRepository.lagreOpptjeningsperiode(behandling, LocalDate.now().minusMonths(10), LocalDate.now().minusDays(1), false);

        // Act 1
        var perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref, stp);
        var perioderVilkår = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(ref, stp);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).toList()).hasSize(1);
        assertThat(perioderVilkår.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILOPP)).toList()).hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.TIL_VURDERING)).toList()).hasSize(1);
        assertThat(perioderVilkår.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.TIL_VURDERING)).toList()).hasSize(1);
        assertThat(perioderVilkår.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.FERDIG_VURDERT_UNDERKJENT)).toList()).hasSize(1);
        assertThat(perioder.stream().filter(OpptjeningsperiodeForSaksbehandling::getErManueltRegistrert).toList()).isEmpty();
        assertThat(perioder.stream().filter(o -> !o.getErManueltRegistrert()).toList()).hasSize(1);
        assertThat(perioderVilkår.stream().filter(o -> !o.getErManueltRegistrert()).toList()).hasSize(2);

        // Act 2
        var saksbehandlet = InntektArbeidYtelseAggregatBuilder
                .oppdatere(Optional.empty(), VersjonType.SAKSBEHANDLET);
        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandlet);
        perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref, stp);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).toList())
                .hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.UNDERKJENT)).toList())
                .hasSize(1);
        var frilansPeriode = perioder.stream()
                .filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).findFirst().get();
        assertThat(frilansPeriode.erManueltBehandlet()).isTrue();
    }

    @Test
    void skal_returnere_oat_frilans_ved_bekreftet_frilans_for_vilkår() {
        // Arrange
        var behandling = opprettBehandling();

        var arbeidsgiver = Arbeidsgiver.virksomhet(ORG_NUMMER);

        var fraOgMed = LocalDate.now().minusMonths(4);
        var tilOgMed = LocalDate.now().minusMonths(3);
        var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);

        var bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1,
                ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER, BigDecimal.TEN, arbeidsgiver);
        opprettInntektForFrilanser(bekreftet, AKTØRID, ARBEIDSFORHOLD_ID, periode1, arbeidsgiver);
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        var ref = BehandlingReferanse.fra(behandling);
        var stp = medUtledetSkjæringstidspunkt(skjæringstidspunkt);
        opptjeningRepository.lagreOpptjeningsperiode(behandling, LocalDate.now().minusMonths(10), LocalDate.now().minusDays(1), false);

        // Act 1
        var perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(ref, stp);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILOPP)).toList())
                .hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.TIL_VURDERING))
                .toList())
                        .hasSize(1);

        // Act 2
        var saksbehandlet = opprettOverstyrtOppgittOpptjening(periode1, ArbeidType.FRILANSER, AKTØRID,
                VersjonType.SAKSBEHANDLET);
        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandlet);
        perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(ref, stp);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).toList())
                .hasSize(1);
        assertThat(
                perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.FERDIG_VURDERT_GODKJENT)).toList())
                        .hasSize(1);
    }

    @Test
    void skal_returnere_en_periode_med_fiktivt_bekreftet_arbeidsforhold() {
        // Arrange
        var behandling = opprettBehandling();
        var ref = BehandlingReferanse.fra(behandling);
        var fraOgMed = LocalDate.of(2015, 1, 4);
        var tilOgMed = skjæringstidspunkt.plusMonths(2);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);
        var saksbehandlet = lagFiktivtArbeidsforholdSaksbehandlet(periode);
        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandlet);
        var informasjon = lagFiktivtArbeidsforholdOverstyring(fraOgMed, tilOgMed);
        iayTjeneste.lagreOverstyrtArbeidsforhold(behandling.getId(), informasjon);

        // Act
        var perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref, medUtledetSkjæringstidspunkt(skjæringstidspunkt));

        // Assert
        assertThat(perioder).hasSize(1);
    }

    @Test
    void skal_kunne_bygge_opptjeningsperiode_basert_på_arbeidsforhold_lagt_til_avsaksbehandler() {
        var behandling = opprettBehandling();
        var ref = BehandlingReferanse.fra(behandling);
        var start = LocalDate.now().minusMonths(5);

        var arbeidsforholdInformasjonBuilder = ArbeidsforholdInformasjonBuilder.builder(Optional.empty());
        var virksomhet = Arbeidsgiver.virksomhet("999999999");

        var arbeidsforholdOverstyringBuilder = arbeidsforholdInformasjonBuilder.getOverstyringBuilderFor(virksomhet,
                null);
        arbeidsforholdOverstyringBuilder.medHandling(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING);
        arbeidsforholdOverstyringBuilder.leggTilOverstyrtPeriode(start, LocalDate.MAX);
        arbeidsforholdOverstyringBuilder.medAngittStillingsprosent(Stillingsprosent.ZERO);
        arbeidsforholdInformasjonBuilder.leggTil(arbeidsforholdOverstyringBuilder);

        iayTjeneste.lagreOverstyrtArbeidsforhold(behandling.getId(), arbeidsforholdInformasjonBuilder);

        var perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref, medUtledetSkjæringstidspunkt(skjæringstidspunkt));
        assertThat(perioder).isNotEmpty();

    }

    private ArbeidsforholdInformasjonBuilder lagFiktivtArbeidsforholdOverstyring(LocalDate fraOgMed, LocalDate tilOgMed) {
        var arbeidsgiver = Arbeidsgiver.virksomhet(KUNSTIG_ORG);
        return ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty())
                .leggTil(ArbeidsforholdOverstyringBuilder.oppdatere(Optional.empty())
                        .medArbeidsgiver(arbeidsgiver)
                        .medHandling(ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER)
                        .leggTilOverstyrtPeriode(fraOgMed, tilOgMed)
                        .medAngittStillingsprosent(new Stillingsprosent(BigDecimal.valueOf(100))));
    }

    private InntektArbeidYtelseAggregatBuilder lagFiktivtArbeidsforholdSaksbehandlet(DatoIntervallEntitet periode) {
        var arbeidsgiver = Arbeidsgiver.virksomhet(KUNSTIG_ORG);
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.SAKSBEHANDLET);
        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(AKTØRID);
        var yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
                new Opptjeningsnøkkel(null, KUNSTIG_ORG, null), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();
        var aktivitetsAvtale = aktivitetsAvtaleBuilder
                .medPeriode(periode)
                .medProsentsats(BigDecimal.valueOf(100))
                .medBeskrivelse("Ser greit ut");
        var ansettelsesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
                .medPeriode(periode);
        yrkesaktivitetBuilder
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidsforholdId(null)
                .leggTilAktivitetsAvtale(aktivitetsAvtale)
                .leggTilAktivitetsAvtale(ansettelsesperiode);
        var aktørArbeid = aktørArbeidBuilder
                .leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        builder.leggTilAktørArbeid(aktørArbeid);
        return builder;
    }

    private InntektArbeidYtelseAggregatBuilder opprettOverstyrtOppgittOpptjening(DatoIntervallEntitet periode, ArbeidType type, AktørId aktørId,
            VersjonType register) {
        var builder = InntektArbeidYtelseAggregatBuilder
                .oppdatere(Optional.empty(), register);

        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(aktørId);
        var yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForType(type);

        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();

        var aktivitetsAvtale = aktivitetsAvtaleBuilder
                .medPeriode(periode)
                .medBeskrivelse("Ser greit ut");

        yrkesaktivitetBuilder
                .medArbeidType(type)
                .leggTilAktivitetsAvtale(aktivitetsAvtale);
        var aktørArbeid = aktørArbeidBuilder
                .leggTilYrkesaktivitet(yrkesaktivitetBuilder);

        builder.leggTilAktørArbeid(aktørArbeid);

        return builder;
    }

    private Behandling opprettBehandling() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AKTØRID));
        var fagsakId = fagsakRepository.opprettNy(fagsak);
        fagsakRepository.oppdaterRelasjonsRolle(fagsakId, RelasjonsRolleType.MORA);
        var builder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = builder.build();
        Behandlingsresultat.opprettFor(behandling);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        var nyttResultat = VilkårResultat.builder().buildFor(behandling);
        behandlingRepository.lagre(nyttResultat, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }

    private InntektArbeidYtelseAggregatBuilder opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AktørId aktørId, InternArbeidsforholdRef ref,
            DatoIntervallEntitet periode, ArbeidType type,
            BigDecimal prosentsats, Arbeidsgiver virksomhet1) {
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);

        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(aktørId);
        var yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
                new Opptjeningsnøkkel(ref, virksomhet1.getIdentifikator(), null), type);

        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder(periode, false);
        var permisjonBuilder = yrkesaktivitetBuilder.getPermisjonBuilder();

        var aktivitetsAvtale = aktivitetsAvtaleBuilder
                .medProsentsats(prosentsats)
                .medBeskrivelse("Ser greit ut");
        var ansettelsesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder(periode, true);

        var permisjon = permisjonBuilder
                .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.UTDANNINGSPERMISJON)
                .medPeriode(periode.getFomDato(), periode.getTomDato())
                .medProsentsats(BigDecimal.valueOf(100))
                .build();

        yrkesaktivitetBuilder
                .medArbeidType(type)
                .medArbeidsgiver(virksomhet1)
                .medArbeidsforholdId(ARBEIDSFORHOLD_ID)
                .leggTilPermisjon(permisjon)
                .leggTilAktivitetsAvtale(aktivitetsAvtale)
                .leggTilAktivitetsAvtale(ansettelsesperiode);

        var aktørArbeid = aktørArbeidBuilder
                .leggTilYrkesaktivitet(yrkesaktivitetBuilder);

        builder.leggTilAktørArbeid(aktørArbeid);

        return builder;
    }

    private void opprettInntektForFrilanser(InntektArbeidYtelseAggregatBuilder bekreftet, AktørId aktørId, InternArbeidsforholdRef ref,
            DatoIntervallEntitet periode,
            Arbeidsgiver virksomhet1) {
        var ainntektBuilder = bekreftet.getAktørInntektBuilder(aktørId);
        var inntektBuilder = ainntektBuilder.getInntektBuilder(InntektsKilde.INNTEKT_OPPTJENING,
                new Opptjeningsnøkkel(ref, virksomhet1.getIdentifikator(), null));
        inntektBuilder.medArbeidsgiver(virksomhet1);
        inntektBuilder.leggTilInntektspost(InntektspostBuilder.ny().medInntektspostType(InntektspostType.LØNN)
                .medPeriode(periode.getFomDato(), periode.getTomDato()).medBeløp(BigDecimal.TEN));
        ainntektBuilder.leggTilInntekt(inntektBuilder);
        bekreftet.leggTilAktørInntekt(ainntektBuilder);
    }

    private Skjæringstidspunkt medUtledetSkjæringstidspunkt(LocalDate stp) {
        return Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).build();
    }

}
