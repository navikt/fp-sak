package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import static java.util.Arrays.asList;
import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.opptjening.dto.BekreftOpptjeningPeriodeDto;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

@ExtendWith(JpaExtension.class)
class BekreftOpptjeningPeriodeAksjonspunktTest {

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;

    private BekreftOpptjeningPeriodeAksjonspunkt bekreftOpptjeningPeriodeAksjonspunkt;

    private final AktørId AKTØRID = AktørId.dummy();
    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final AksjonspunktutlederForVurderOppgittOpptjening vurderOpptjening = mock(AksjonspunktutlederForVurderOppgittOpptjening.class);
    private static final String NAV_ORGNR = "889640782";
    private static final Arbeidsgiver ARBEIDSGIVER = Arbeidsgiver.virksomhet(NAV_ORGNR);
    private static final InternArbeidsforholdRef REF = InternArbeidsforholdRef.nyRef();


    @BeforeEach
    void oppsett(EntityManager entityManager) {
        behandlingRepository = new BehandlingRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        var tjeneste = mock(VirksomhetTjeneste.class);
        var builder = new Virksomhet.Builder();
        var børreAs = builder.medOrgnr(KUNSTIG_ORG)
                .medNavn("Børre AS")
                .build();
        Mockito.when(tjeneste.finnOrganisasjon(ArgumentMatchers.any())).thenReturn(Optional.of(børreAs));
        bekreftOpptjeningPeriodeAksjonspunkt = new BekreftOpptjeningPeriodeAksjonspunkt(iayTjeneste, vurderOpptjening);
    }

    @Test
    void skal_lagre_ned_bekreftet_kunstig_arbeidsforhold() {
        var iDag = LocalDate.now();
        var behandling = opprettBehandling();

        var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusMonths(3), iDag.minusMonths(2));

        iayTjeneste.lagreOverstyrtArbeidsforhold(behandling.getId(), ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty())
                .leggTil(ArbeidsforholdOverstyringBuilder
                        .oppdatere(Optional.empty())
                        .leggTilOverstyrtPeriode(periode1.getFomDato(), periode1.getTomDato())
                        .medAngittStillingsprosent(new Stillingsprosent(100))
                        .medArbeidsforholdRef(InternArbeidsforholdRef.nullRef())
                        .medArbeidsgiver(Arbeidsgiver.virksomhet(KUNSTIG_ORG))
                        .medAngittArbeidsgiverNavn("Ambassade")));

        // simulerer svar fra GUI
        var dto = new BekreftOpptjeningPeriodeDto();
        dto.setAktivitetType(OpptjeningAktivitetType.ARBEID);
        dto.setArbeidsforholdRef(InternArbeidsforholdRef.nullRef().getReferanse());
        dto.setArbeidsgiverNavn("Ambassade");
        dto.setArbeidsgiverReferanse(KUNSTIG_ORG);
        dto.setOpptjeningFom(periode1.getFomDato());
        dto.setOpptjeningTom(periode1.getTomDato());
        dto.setErGodkjent(true);
        dto.setBegrunnelse("Ser greit ut");

        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(iDag).build();

        // Act
        bekreftOpptjeningPeriodeAksjonspunkt.oppdater(behandling.getId(), behandling.getAktørId(), Collections.singletonList(dto),
                skjæringstidspunkt);

        var grunnlag = hentGrunnlag(behandling);
        assertThat(grunnlag.getBekreftetAnnenOpptjening(behandling.getAktørId())).isPresent();

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getBekreftetAnnenOpptjening(behandling.getAktørId()));
        var yrkesaktiviteter = filter.getYrkesaktiviteter();

        assertThat(yrkesaktiviteter).hasSize(1);
        var perioder = filter.getAktivitetsAvtalerForArbeid().stream().map(AktivitetsAvtale::getPeriode)
                .toList();
        assertThat(perioder).contains(periode1);
    }

    @Test
    void skal_lagre_ned_bekreftet_aksjonspunkt() {
        var iDag = LocalDate.now();
        var behandling = opprettBehandling();

        var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusMonths(3), iDag.minusMonths(2));
        var periode1_2 = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusMonths(2), iDag.minusMonths(1));

        var oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode1, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode1, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode1_2, ArbeidType.ETTERLØNN_SLUTTPAKKE));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        // simulerer svar fra GUI
        var periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusMonths(2), iDag.minusMonths(1));
        var dto = new BekreftOpptjeningPeriodeDto();
        dto.setAktivitetType(OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE);
        dto.setOpptjeningFom(periode2.getFomDato());
        dto.setOpptjeningTom(periode2.getTomDato());
        dto.setErGodkjent(true);
        dto.setBegrunnelse("Ser greit ut");
        var dto2 = new BekreftOpptjeningPeriodeDto();
        dto2.setAktivitetType(OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE);
        dto2.setOpptjeningFom(periode1_2.getFomDato());
        dto2.setOpptjeningTom(periode1_2.getTomDato());
        dto2.setErGodkjent(false);
        dto2.setBegrunnelse("Ser greit ut");
        dto2.setArbeidsgiverNavn("test");
        var dto3 = new BekreftOpptjeningPeriodeDto();
        dto3.setAktivitetType(OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE);
        dto3.setOpptjeningFom(periode1.getFomDato());
        dto3.setOpptjeningTom(periode1.getTomDato());
        dto3.setErGodkjent(true);
        dto3.setBegrunnelse("Ser greit ut");

        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(iDag).build();

        // Act
        bekreftOpptjeningPeriodeAksjonspunkt.oppdater(behandling.getId(), behandling.getAktørId(), asList(dto, dto2, dto3), skjæringstidspunkt);

        var grunnlag = hentGrunnlag(behandling);
        assertThat(grunnlag.getBekreftetAnnenOpptjening(behandling.getAktørId())).isPresent();

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getBekreftetAnnenOpptjening(behandling.getAktørId()));
        var yrkesaktiviteter = filter.getYrkesaktiviteter();

        assertThat(yrkesaktiviteter).hasSize(1);
        var perioder = filter.getAktivitetsAvtalerForArbeid().stream().map(AktivitetsAvtale::getPeriode)
                .toList();
        assertThat(perioder).contains(periode1, periode2);
    }

    @Test
    void skal_lagre_ned_delvis_godkjent_arbeid() {
        var behandling = opprettBehandling();

        var avtale1Fom = LocalDate.of(2022,12,1);
        var avtale2Fom = LocalDate.of(2023,8,1);
        var ansFom = LocalDate.of(2023,4,1);
        var ansTom = LocalDate.of(2024,1,1);

        var builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        var arbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        var nøkkel = Opptjeningsnøkkel.forArbeidsforholdIdMedArbeidgiver(REF, ARBEIDSGIVER);
        var yrkesaktivitetBuilderForType = arbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(nøkkel, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktivitetBuilderForType.medArbeidsgiver(ARBEIDSGIVER)
            .medArbeidsforholdId(REF)
            .leggTilAktivitetsAvtale(
                yrkesaktivitetBuilderForType.getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(avtale1Fom, avtale2Fom.minusDays(1)), false)
                    .medSisteLønnsendringsdato(LocalDate.now().minusMonths(3))
                    .medProsentsats(BigDecimal.ZERO))
            .leggTilAktivitetsAvtale(
                yrkesaktivitetBuilderForType.getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMed(avtale2Fom), false)
                    .medSisteLønnsendringsdato(LocalDate.now().minusMonths(3))
                    .medProsentsats(BigDecimal.ZERO))
            .leggTilAktivitetsAvtale(yrkesaktivitetBuilderForType
                .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(ansFom, ansTom), true));
        arbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilderForType);
        builder.leggTilAktørArbeid(arbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);


        // simulerer svar fra GUI
        var dto = new BekreftOpptjeningPeriodeDto();
        dto.setAktivitetType(OpptjeningAktivitetType.ARBEID);
        dto.setOpptjeningFom(avtale1Fom);
        dto.setOpptjeningTom(ansFom.minusDays(1));
        dto.setArbeidsgiverReferanse(ARBEIDSGIVER.getIdentifikator());
        dto.setArbeidsforholdRef(REF.getReferanse());
        dto.setErGodkjent(false);
        dto.setBegrunnelse("Ser ugreit ut");
        var dto2 = new BekreftOpptjeningPeriodeDto();
        dto2.setAktivitetType(OpptjeningAktivitetType.ARBEID);
        dto2.setOpptjeningFom(ansFom);
        dto2.setOpptjeningTom(avtale2Fom.minusDays(1));
        dto2.setArbeidsgiverReferanse(ARBEIDSGIVER.getIdentifikator());
        dto2.setArbeidsforholdRef(REF.getReferanse());
        dto2.setErGodkjent(true);
        dto2.setBegrunnelse("Ser greit ut");
        var dto3 = new BekreftOpptjeningPeriodeDto();
        dto3.setAktivitetType(OpptjeningAktivitetType.ARBEID);
        dto3.setOpptjeningFom(avtale2Fom);
        dto3.setOpptjeningTom(ansTom.minusMonths(1));
        dto3.setArbeidsgiverReferanse(ARBEIDSGIVER.getIdentifikator());
        dto3.setArbeidsforholdRef(REF.getReferanse());
        dto3.setErGodkjent(false);
        dto3.setBegrunnelse("Ser ugreit ut");

        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(ansTom.minusMonths(1).plusDays(1)).build();

        // Act
        bekreftOpptjeningPeriodeAksjonspunkt.oppdater(behandling.getId(), behandling.getAktørId(), asList(dto, dto2, dto3), skjæringstidspunkt);

        var oppdatert = hentGrunnlag(behandling);

        var filterSaksbehandlet = new YrkesaktivitetFilter(oppdatert.getArbeidsforholdInformasjon(), oppdatert.getBekreftetAnnenOpptjening(behandling.getAktørId()));
        var yrkesaktiviteter = filterSaksbehandlet.getYrkesaktiviteter();
        var ansettelsesperioder = yrkesaktiviteter.stream()
            .flatMap(y -> y.getAlleAktivitetsAvtaler().stream().filter(AktivitetsAvtale::erAnsettelsesPeriode))
            .collect(Collectors.toList());

        assertThat(yrkesaktiviteter).hasSize(1);
        assertThat(ansettelsesperioder).hasSize(2);
        var bekreftet = ansettelsesperioder.stream().filter(p -> p.getPeriode().getFomDato().equals(ansFom)).findFirst().orElseThrow();
        assertThat(bekreftet.getPeriode().getFomDato()).isEqualTo(ansFom);
        assertThat(bekreftet.getPeriode().getTomDato()).isEqualTo(avtale2Fom.minusDays(1));
        assertThat(bekreftet.getBeskrivelse()).isEqualTo("Ser greit ut");
        var siste = ansettelsesperioder.stream().filter(p -> p.getPeriode().getTomDato().equals(ansTom)).findFirst().orElseThrow();
        assertThat(siste.getPeriode().getFomDato()).isEqualTo(ansTom.minusMonths(1).plusDays(1));
        assertThat(siste.getPeriode().getTomDato()).isEqualTo(ansTom);
        assertThat(siste.getBeskrivelse()).isNull();
    }

    @Test
    void skal_lagre_endring_i_periode_for_egen_næring() {
        var iDag = LocalDate.now();
        var behandling = opprettBehandling();

        when(vurderOpptjening.girAksjonspunktForOppgittNæring(any(), any(), any(), any())).thenReturn(true);
        var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusMonths(3), iDag.minusMonths(2));
        var periode1_2 = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusMonths(2), iDag.minusMonths(2));

        var oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilEgenNæring(List.of(OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
            .medPeriode(periode1)));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        var dto = new BekreftOpptjeningPeriodeDto();
        dto.setAktivitetType(OpptjeningAktivitetType.NÆRING);
        dto.setOpptjeningFom(periode1_2.getFomDato());
        dto.setOpptjeningTom(periode1_2.getTomDato());
        dto.setErGodkjent(true);
        dto.setBegrunnelse("Ser greit ut");

        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(iDag).build();

        // Act
        bekreftOpptjeningPeriodeAksjonspunkt.oppdater(behandling.getId(), behandling.getAktørId(), List.of(dto), skjæringstidspunkt);
        var grunnlag = hentGrunnlag(behandling);
        assertThat(grunnlag.getBekreftetAnnenOpptjening(behandling.getAktørId())).isPresent();

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getBekreftetAnnenOpptjening(behandling.getAktørId()));
        var yrkesaktiviteter = filter.getYrkesaktiviteter();

        assertThat(yrkesaktiviteter).hasSize(1);
        var aktivitetsAvtale = filter.getAktivitetsAvtalerForArbeid().iterator().next();
        assertThat(DatoIntervallEntitet.fraOgMedTilOgMed(aktivitetsAvtale.getPeriode().getFomDato(), aktivitetsAvtale.getPeriode().getTomDato()))
                .isEqualTo(periode1_2);
    }

    private InntektArbeidYtelseGrunnlag hentGrunnlag(final Behandling behandling) {
        return iayTjeneste.finnGrunnlag(behandling.getId()).orElseThrow();
    }

    private Behandling opprettBehandling() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AKTØRID));
        fagsakRepository.opprettNy(fagsak);
        var builder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }
}
