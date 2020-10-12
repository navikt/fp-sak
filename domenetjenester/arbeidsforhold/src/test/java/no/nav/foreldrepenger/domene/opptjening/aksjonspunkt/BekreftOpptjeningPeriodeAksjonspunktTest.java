package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import static java.util.Arrays.asList;
import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.opptjening.dto.BekreftOpptjeningPeriodeDto;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class BekreftOpptjeningPeriodeAksjonspunktTest {
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private BehandlingRepository behandlingRepository = new BehandlingRepository(repoRule.getEntityManager());
    private FagsakRepository fagsakRepository = new FagsakRepository(repoRule.getEntityManager());
    private VirksomhetTjeneste tjeneste;

    private BekreftOpptjeningPeriodeAksjonspunkt bekreftOpptjeningPeriodeAksjonspunkt;

    private AktørId AKTØRID = AktørId.dummy();
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final AksjonspunktutlederForVurderOppgittOpptjening vurderOpptjening = mock(AksjonspunktutlederForVurderOppgittOpptjening.class);

    @Before
    public void oppsett() {
        tjeneste = mock(VirksomhetTjeneste.class);
        Virksomhet.Builder builder = new Virksomhet.Builder();
        Virksomhet børreAs = builder.medOrgnr(KUNSTIG_ORG)
            .medNavn("Børre AS")
            .build();
        Mockito.when(tjeneste.finnOrganisasjon(Mockito.any())).thenReturn(Optional.of(børreAs));
        bekreftOpptjeningPeriodeAksjonspunkt = new BekreftOpptjeningPeriodeAksjonspunkt(iayTjeneste, vurderOpptjening);
    }

    @Test
    public void skal_lagre_ned_bekreftet_kunstig_arbeidsforhold() {
        LocalDate iDag = LocalDate.now();
        final Behandling behandling = opprettBehandling(iDag);

        DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusMonths(3), iDag.minusMonths(2));

        iayTjeneste.lagreArbeidsforhold(behandling.getId(), AKTØRID, ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty())
        .leggTil(ArbeidsforholdOverstyringBuilder
            .oppdatere(Optional.empty())
            .leggTilOverstyrtPeriode(periode1.getFomDato(), periode1.getTomDato())
            .medAngittStillingsprosent(new Stillingsprosent(100))
            .medArbeidsforholdRef(InternArbeidsforholdRef.nullRef())
            .medArbeidsgiver(Arbeidsgiver.virksomhet(KUNSTIG_ORG))
            .medAngittArbeidsgiverNavn("Ambassade")));

        // simulerer svar fra GUI
        BekreftOpptjeningPeriodeDto dto = new BekreftOpptjeningPeriodeDto();
        dto.setAktivitetType(OpptjeningAktivitetType.ARBEID);
        dto.setArbeidsforholdRef(InternArbeidsforholdRef.nullRef().getReferanse());
        dto.setArbeidsgiverNavn("Ambassade");
        dto.setArbeidsgiverIdentifikator(KUNSTIG_ORG);
        dto.setOriginalTom(periode1.getTomDato());
        dto.setOriginalFom(periode1.getFomDato());
        dto.setOpptjeningFom(periode1.getFomDato());
        dto.setOpptjeningTom(periode1.getTomDato());
        dto.setErGodkjent(true);
        dto.setErEndret(false);
        dto.setBegrunnelse("Ser greit ut");


        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(iDag).build();

        //Act
        bekreftOpptjeningPeriodeAksjonspunkt.oppdater(behandling.getId(), behandling.getAktørId(), Collections.singletonList(dto), skjæringstidspunkt);

        InntektArbeidYtelseGrunnlag grunnlag = hentGrunnlag(behandling);
        assertThat(grunnlag.getBekreftetAnnenOpptjening(behandling.getAktørId())).isPresent();

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getBekreftetAnnenOpptjening(behandling.getAktørId()));
        Collection<Yrkesaktivitet> yrkesaktiviteter = filter.getYrkesaktiviteter();

        assertThat(yrkesaktiviteter).hasSize(1);
        final List<DatoIntervallEntitet> perioder = filter.getAktivitetsAvtalerForArbeid().stream().map(AktivitetsAvtale::getPeriode).collect(Collectors.toList());
        assertThat(perioder).contains(periode1);
    }


    @Test
    public void skal_lagre_ned_bekreftet_aksjonspunkt() {
        LocalDate iDag = LocalDate.now();
        final Behandling behandling = opprettBehandling(iDag);

        DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusMonths(3), iDag.minusMonths(2));
        DatoIntervallEntitet periode1_2 = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusMonths(2), iDag.minusMonths(1));

        OppgittOpptjeningBuilder oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode1, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode1, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode1_2, ArbeidType.ETTERLØNN_SLUTTPAKKE));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        // simulerer svar fra GUI
        DatoIntervallEntitet periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusMonths(2), iDag.minusMonths(1));
        BekreftOpptjeningPeriodeDto dto = new BekreftOpptjeningPeriodeDto();
        dto.setAktivitetType(OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE);
        dto.setOriginalTom(periode1.getTomDato());
        dto.setOriginalFom(periode1.getFomDato());
        dto.setOpptjeningFom(periode2.getFomDato());
        dto.setOpptjeningTom(periode2.getTomDato());
        dto.setErGodkjent(true);
        dto.setErEndret(true);
        dto.setBegrunnelse("Ser greit ut");
        BekreftOpptjeningPeriodeDto dto2 = new BekreftOpptjeningPeriodeDto();
        dto2.setAktivitetType(OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE);
        dto2.setOpptjeningFom(periode1_2.getFomDato());
        dto2.setOpptjeningTom(periode1_2.getTomDato());
        dto2.setOriginalFom(periode1_2.getFomDato());
        dto2.setOriginalTom(periode1_2.getTomDato());
        dto2.setErGodkjent(false);
        dto2.setBegrunnelse("Ser greit ut");
        dto2.setArbeidsgiverIdentifikator("test");
        dto2.setArbeidsgiverNavn("test");
        BekreftOpptjeningPeriodeDto dto3 = new BekreftOpptjeningPeriodeDto();
        dto3.setAktivitetType(OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE);
        dto3.setOriginalTom(periode1.getTomDato());
        dto3.setOriginalFom(periode1.getFomDato());
        dto3.setOpptjeningFom(periode1.getFomDato());
        dto3.setOpptjeningTom(periode1.getTomDato());
        dto3.setErGodkjent(true);
        dto3.setBegrunnelse("Ser greit ut");

        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(iDag).build();

        //Act
        bekreftOpptjeningPeriodeAksjonspunkt.oppdater(behandling.getId(), behandling.getAktørId(), asList(dto, dto2, dto3), skjæringstidspunkt);

        InntektArbeidYtelseGrunnlag grunnlag = hentGrunnlag(behandling);
        assertThat(grunnlag.getBekreftetAnnenOpptjening(behandling.getAktørId())).isPresent();

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getBekreftetAnnenOpptjening(behandling.getAktørId()));
        Collection<Yrkesaktivitet> yrkesaktiviteter = filter.getYrkesaktiviteter();

        assertThat(yrkesaktiviteter).hasSize(1);
        final List<DatoIntervallEntitet> perioder = filter.getAktivitetsAvtalerForArbeid().stream().map(AktivitetsAvtale::getPeriode).collect(Collectors.toList());
        assertThat(perioder).contains(periode1, periode2);
    }

    @Test
    public void skal_lagre_endring_i_periode_for_egen_næring() {
        LocalDate iDag = LocalDate.now();
        final Behandling behandling = opprettBehandling(iDag);

        when(vurderOpptjening.girAksjonspunktForOppgittNæring(any(), any(), any(), any())).thenReturn(true);
        DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusMonths(3), iDag.minusMonths(2));
        DatoIntervallEntitet periode1_2 = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusMonths(2), iDag.minusMonths(2));

        OppgittOpptjeningBuilder oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilEgneNæringer(asList(OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
            .medPeriode(periode1)));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        BekreftOpptjeningPeriodeDto dto = new BekreftOpptjeningPeriodeDto();
        dto.setAktivitetType(OpptjeningAktivitetType.NÆRING);
        dto.setOriginalTom(periode1.getTomDato());
        dto.setOriginalFom(periode1.getFomDato());
        dto.setOpptjeningFom(periode1_2.getFomDato());
        dto.setOpptjeningTom(periode1_2.getTomDato());
        dto.setErGodkjent(true);
        dto.setErEndret(true);
        dto.setBegrunnelse("Ser greit ut");

        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(iDag).build();

        //Act
        bekreftOpptjeningPeriodeAksjonspunkt.oppdater(behandling.getId(), behandling.getAktørId(), asList(dto), skjæringstidspunkt);
        InntektArbeidYtelseGrunnlag grunnlag = hentGrunnlag(behandling);
        assertThat(grunnlag.getBekreftetAnnenOpptjening(behandling.getAktørId())).isPresent();

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getBekreftetAnnenOpptjening(behandling.getAktørId()));
        Collection<Yrkesaktivitet> yrkesaktiviteter = filter.getYrkesaktiviteter();

        assertThat(yrkesaktiviteter).hasSize(1);
        AktivitetsAvtale aktivitetsAvtale = filter.getAktivitetsAvtalerForArbeid().iterator().next();
        assertThat(DatoIntervallEntitet.fraOgMedTilOgMed(aktivitetsAvtale.getPeriode().getFomDato(), aktivitetsAvtale.getPeriode().getTomDato())).isEqualTo(periode1_2);
    }

    private InntektArbeidYtelseGrunnlag hentGrunnlag(final Behandling behandling) {
        return iayTjeneste.finnGrunnlag(behandling.getId()).orElseThrow();
    }

    private Behandling opprettBehandling(LocalDate iDag) {
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(AKTØRID, Språkkode.NB));
        fagsakRepository.opprettNy(fagsak);
        final Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        final Behandling behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }
}
