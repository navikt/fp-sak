package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.person.PersonIdentTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyrtePerioder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.tid.AbstractLocalDateInterval;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.konfig.Tid;

@RunWith(CdiRunner.class)
public class AvklarArbeidsforholdOppdatererTest {

    private static final String NAV_ORGNR = "889640782";
    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_REF = InternArbeidsforholdRef.namedRef("TEST-REF");
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private IAYRepositoryProvider repositoryProvider = new IAYRepositoryProvider(repoRule.getEntityManager());

    @Inject
    private HistorikkTjenesteAdapter historikkAdapter;
    @Inject
    private PersonIdentTjeneste tpsTjeneste;
    @Inject
    private VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste;

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private AvklarArbeidsforholdOppdaterer oppdaterer;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);
    private String randomId = UUID.randomUUID().toString();
    private VirksomhetTjeneste virksomhetTjeneste = mock(VirksomhetTjeneste.class);

    @Before
    public void oppsett() {
        initMocks(this);
        ArbeidsgiverTjeneste arbeidsgiverTjeneste = new ArbeidsgiverTjeneste(tpsTjeneste, virksomhetTjeneste);
        ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste = new ArbeidsforholdAdministrasjonTjeneste(
            vurderArbeidsforholdTjeneste,
            arbeidsgiverTjeneste,
            inntektsmeldingTjeneste,
            iayTjeneste);
        var arbeidsgiverHistorikkinnslagTjeneste = new ArbeidsgiverHistorikkinnslag(arbeidsgiverTjeneste);
        ArbeidsforholdHistorikkinnslagTjeneste arbeidsforholdHistorikkinnslagTjeneste = new ArbeidsforholdHistorikkinnslagTjeneste(historikkAdapter, arbeidsgiverHistorikkinnslagTjeneste);
        oppdaterer = new AvklarArbeidsforholdOppdaterer(
            arbeidsforholdAdministrasjonTjeneste,
            iayTjeneste,
            arbeidsforholdHistorikkinnslagTjeneste);
    }

    @Test
    public void skal_kreve_totrinn_hvis_saksbehandler_har_tatt_stilling_til_aksjonspunktet() {

        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        opprettIAYAggregat(behandling, false, LocalDate.of(2018, 1, 1));

        Aksjonspunkt aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);

        AvklarArbeidsforholdDto avklarArbeidsforholdDto = new AvklarArbeidsforholdDto("Har tatt stilling til dette", List.of());
        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.of(2019, 1, 1)).build();

        // Act
        OppdateringResultat resultat = oppdaterer.oppdater(avklarArbeidsforholdDto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, skjæringstidspunkt, avklarArbeidsforholdDto.getBegrunnelse()));

        // Assert
        BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
        Behandling behandling1 = behandlingRepository.hentBehandling(behandling.getId());
        Set<Aksjonspunkt> aksjonspunkter = behandling1.getAksjonspunkter();
        assertThat(aksjonspunkter).hasSize(1);
        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    public void skal_kunne_legge_til_nytt_arbeidsforhold() {
        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        opprettTomtIAYAggregat(behandling);

        //simulere at 5080 har oppstått
        Aksjonspunkt aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);

        LocalDate stp = LocalDate.of(2019, 1, 1);

        ArbeidsforholdDto nyttArbeidsforhod = new ArbeidsforholdDto();
        String navn = "Utlandet";
        nyttArbeidsforhod.setNavn(navn);
        LocalDate fomDato = stp.minusYears(3);
        nyttArbeidsforhod.setFomDato(fomDato);
        BigDecimal stillingsprosent = BigDecimal.valueOf(100);
        nyttArbeidsforhod.setStillingsprosent(stillingsprosent);
        nyttArbeidsforhod.setLagtTilAvSaksbehandler(true);
        nyttArbeidsforhod.setBrukArbeidsforholdet(true);

        List<ArbeidsforholdDto> nyeArbeidsforhold = List.of(nyttArbeidsforhod);
        AvklarArbeidsforholdDto avklarArbeidsforholdDto = new AvklarArbeidsforholdDto("Har lagt til et nytt arbeidsforhold", nyeArbeidsforhold);

        //Act
        oppdaterer.oppdater(avklarArbeidsforholdDto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).build(), avklarArbeidsforholdDto.getBegrunnelse()));

        // Assert
        List<ArbeidsforholdOverstyring> overstyring = hentGrunnlag(behandling).getArbeidsforholdInformasjon()
            .map(ArbeidsforholdInformasjon::getOverstyringer).orElse(Collections.emptyList());

        assertThat(overstyring).hasSize(1);
        ArbeidsforholdOverstyring overstyrtArbeidsforhold = overstyring.get(0);
        assertThat(overstyrtArbeidsforhold.getStillingsprosent()).isEqualTo(new Stillingsprosent(stillingsprosent));
        assertThat(overstyrtArbeidsforhold.getArbeidsgiverNavn()).isEqualTo(navn);
        assertThat(overstyrtArbeidsforhold.getArbeidsforholdOverstyrtePerioder()).hasSize(1);
        assertThat(overstyrtArbeidsforhold.getArbeidsforholdOverstyrtePerioder().get(0).getOverstyrtePeriode()).isEqualByComparingTo(DatoIntervallEntitet.fraOgMed(fomDato));

        AktørId aktørId = behandling.getAktørId();

        // Henter opp yrkesaktivitet med overstyring
        InntektArbeidYtelseGrunnlag grunnlag = hentGrunnlag(behandling);
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId)).før(stp);
        Collection<Yrkesaktivitet> yrkesaktiviteter = filter.getYrkesaktiviteter();
        Yrkesaktivitet yrkesaktivitet = yrkesaktiviteter.iterator().next();
        List<AktivitetsAvtale> ansettelsesPerioder = filter.getAnsettelsesPerioder(yrkesaktivitet);
        assertThat(ansettelsesPerioder).hasSize(1);
        assertThat(ansettelsesPerioder.get(0).getPeriode().getFomDato()).isEqualTo(fomDato);
        Collection<AktivitetsAvtale> aktivitetsAvtaler = filter.getAktivitetsAvtalerForArbeid();
        assertThat(aktivitetsAvtaler).hasSize(1);
        assertThat(aktivitetsAvtaler.iterator().next().getProsentsats().getVerdi()).isEqualByComparingTo(stillingsprosent);
    }

    @Test
    public void skal_kunne_legge_til_arbeidsforhold_basert_på_inntektsmelding() {
        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        opprettTomtIAYAggregat(behandling);

        //simulere at 5080 har oppstått
        Aksjonspunkt aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);

        LocalDate stp = LocalDate.of(2019, 1, 1);

        ArbeidsforholdDto nyttArbeidsforhod = new ArbeidsforholdDto();
        String navn = "Utlandet";
        nyttArbeidsforhod.setNavn(navn);
        LocalDate fomDato = stp.minusYears(3);
        nyttArbeidsforhod.setFomDato(fomDato);
        BigDecimal stillingsprosent = BigDecimal.valueOf(100);
        nyttArbeidsforhod.setStillingsprosent(stillingsprosent);
        nyttArbeidsforhod.setLagtTilAvSaksbehandler(false);
        nyttArbeidsforhod.setBasertPaInntektsmelding(true);
        nyttArbeidsforhod.setBrukArbeidsforholdet(true);
        nyttArbeidsforhod.setArbeidsgiverIdentifikator(NAV_ORGNR);

        List<ArbeidsforholdDto> nyeArbeidsforhold = List.of(nyttArbeidsforhod);
        AvklarArbeidsforholdDto avklarArbeidsforholdDto = new AvklarArbeidsforholdDto("Har lagt til et nytt arbeidsforhold", nyeArbeidsforhold);

        //Act
        oppdaterer.oppdater(avklarArbeidsforholdDto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).build(), avklarArbeidsforholdDto.getBegrunnelse()));

        // Assert
        List<ArbeidsforholdOverstyring> overstyring = hentGrunnlag(behandling).getArbeidsforholdInformasjon()
            .map(ArbeidsforholdInformasjon::getOverstyringer).orElse(Collections.emptyList());

        assertThat(overstyring).hasSize(1);
        ArbeidsforholdOverstyring overstyrtArbeidsforhold = overstyring.get(0);
        assertThat(overstyrtArbeidsforhold.getStillingsprosent()).isEqualTo(new Stillingsprosent(stillingsprosent));
        assertThat(overstyrtArbeidsforhold.getArbeidsgiverNavn()).isEqualTo(navn);
        assertThat(overstyrtArbeidsforhold.getArbeidsforholdOverstyrtePerioder()).hasSize(1);
        assertThat(overstyrtArbeidsforhold.getArbeidsforholdOverstyrtePerioder().get(0).getOverstyrtePeriode()).isEqualByComparingTo(DatoIntervallEntitet.fraOgMed(fomDato));

        AktørId aktørId = behandling.getAktørId();

        // Henter opp yrkesaktivitet med overstyring
        InntektArbeidYtelseGrunnlag grunnlag = hentGrunnlag(behandling);
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId)).før(stp);
        Collection<Yrkesaktivitet> yrkesaktiviteter = filter.getYrkesaktiviteter();
        Yrkesaktivitet yrkesaktivitet = yrkesaktiviteter.iterator().next();
        List<AktivitetsAvtale> ansettelsesPerioder = filter.getAnsettelsesPerioder(yrkesaktivitet);
        assertThat(ansettelsesPerioder).hasSize(1);
        assertThat(ansettelsesPerioder.get(0).getPeriode().getFomDato()).isEqualTo(fomDato);
        Collection<AktivitetsAvtale> aktivitetsAvtaler = filter.getAktivitetsAvtalerForArbeid();
        assertThat(aktivitetsAvtaler).hasSize(1);
        assertThat(aktivitetsAvtaler.iterator().next().getProsentsats().getVerdi()).isEqualByComparingTo(stillingsprosent);
    }

    @Test
    public void skal_utlede_handling_lik_BRUK_MED_OVERSTYRT_PERIODE() {
        // Arrange
        String navn = "Arbeidsgiver";
        LocalDate stpDato = LocalDate.of(2019, 1, 1);
        LocalDate fomDato = stpDato.minusYears(3);
        LocalDate tomDato = stpDato.minusMonths(2);
        LocalDate overstyrtTomDato = stpDato.minusMonths(4);
        BigDecimal stillingsprosent = BigDecimal.valueOf(100);
        opprettVirksomhetAG();

        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        opprettIAYAggregat(behandling, false, fomDato);

        Aksjonspunkt aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);

        ArbeidsforholdDto arbeidsforhold = new ArbeidsforholdDto();
        arbeidsforhold.setNavn(navn);
        arbeidsforhold.setFomDato(fomDato);
        arbeidsforhold.setTomDato(tomDato);
        arbeidsforhold.setOverstyrtTom(overstyrtTomDato);
        arbeidsforhold.setStillingsprosent(stillingsprosent);
        arbeidsforhold.setLagtTilAvSaksbehandler(false);
        arbeidsforhold.setId(randomId);
        arbeidsforhold.setArbeidsgiverIdentifikator(NAV_ORGNR);
        arbeidsforhold.setBrukArbeidsforholdet(true);

        List<ArbeidsforholdDto> nyeArbeidsforhold = List.of(arbeidsforhold);
        AvklarArbeidsforholdDto avklarArbeidsforholdDto = new AvklarArbeidsforholdDto("periode overstyrt", nyeArbeidsforhold);

        Skjæringstidspunkt stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(stpDato)
            .build();

        //Act
        oppdaterer.oppdater(avklarArbeidsforholdDto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, stp, avklarArbeidsforholdDto.getBegrunnelse()));

        // Assert
        List<ArbeidsforholdOverstyring> overstyringer = hentGrunnlag(behandling)
            .getArbeidsforholdInformasjon()
            .map(ArbeidsforholdInformasjon::getOverstyringer)
            .orElse(Collections.emptyList());

        assertThat(overstyringer).as("overstyringer").hasSize(1);
        ArbeidsforholdOverstyring overstyrtArbeidsforhold = overstyringer.get(0);
        assertThat(overstyrtArbeidsforhold.getHandling()).isEqualTo(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE);
        List<ArbeidsforholdOverstyrtePerioder> overstyrtePerioder = overstyrtArbeidsforhold.getArbeidsforholdOverstyrtePerioder();
        assertThat(overstyrtePerioder).as("overstyrtePerioder").hasSize(1);
        assertThat(overstyrtePerioder.get(0).getOverstyrtePeriode().getTomDato()).isEqualTo(overstyrtTomDato);
    }

    @Test
    public void prod_case_både_basertim_og_overstyrt_periode() {
        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        opprettIAYAggregatProdCase(behandling, false, LocalDate.now().minusYears(1));

        //simulere at 5080 har oppstått
        Aksjonspunkt aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);

        LocalDate stp = LocalDate.now();
        LocalDate fomDato = stp.minusYears(1);
        final String navikt = "990983666";
        final String annetforetak = "973861778";
        when(virksomhetTjeneste.hentOrganisasjon(navikt)).thenReturn(Virksomhet.getBuilder().medNavn("NAV IKT").medOrgnr(navikt).build());
        when(virksomhetTjeneste.hentOrganisasjon(annetforetak)).thenReturn(Virksomhet.getBuilder().medNavn("Annet foretak").medOrgnr(annetforetak).build());
        when(virksomhetTjeneste.hentOrganisasjon(NAV_ORGNR)).thenReturn(Virksomhet.getBuilder().medNavn("NAV").medOrgnr(NAV_ORGNR).build());

        ArbeidsforholdDto nyttArbeidsforholdFraIM = new ArbeidsforholdDto();
        nyttArbeidsforholdFraIM.setNavn("NAV IKT");
        nyttArbeidsforholdFraIM.setFomDato(fomDato);
        nyttArbeidsforholdFraIM.setStillingsprosent(BigDecimal.valueOf(100));
        nyttArbeidsforholdFraIM.setLagtTilAvSaksbehandler(false);
        nyttArbeidsforholdFraIM.setBasertPaInntektsmelding(true);
        nyttArbeidsforholdFraIM.setBrukArbeidsforholdet(true);
        nyttArbeidsforholdFraIM.setId(UUID.randomUUID().toString());
        nyttArbeidsforholdFraIM.setArbeidsgiverIdentifikator(navikt);

        ArbeidsforholdDto arbeidsforholdZeroProsent = new ArbeidsforholdDto();
        arbeidsforholdZeroProsent.setNavn("Annet foretak");
        arbeidsforholdZeroProsent.setFomDato(fomDato);
        arbeidsforholdZeroProsent.setTomDato(Tid.TIDENES_ENDE);
        arbeidsforholdZeroProsent.setOverstyrtTom(stp.minusMonths(2));
        arbeidsforholdZeroProsent.setStillingsprosent(BigDecimal.ZERO);
        arbeidsforholdZeroProsent.setLagtTilAvSaksbehandler(false);
        arbeidsforholdZeroProsent.setId(UUID.randomUUID().toString());
        arbeidsforholdZeroProsent.setArbeidsgiverIdentifikator(annetforetak);
        arbeidsforholdZeroProsent.setBrukArbeidsforholdet(true);
        arbeidsforholdZeroProsent.setFortsettBehandlingUtenInntektsmelding(true);

        ArbeidsforholdDto normaltArbeidsforhold = new ArbeidsforholdDto();
        normaltArbeidsforhold.setNavn("NAV");
        normaltArbeidsforhold.setFomDato(fomDato);
        normaltArbeidsforhold.setStillingsprosent(BigDecimal.valueOf(100));
        normaltArbeidsforhold.setBrukArbeidsforholdet(true);
        normaltArbeidsforhold.setId(randomId);
        normaltArbeidsforhold.setArbeidsgiverIdentifikator(NAV_ORGNR);

        List<ArbeidsforholdDto> nyeArbeidsforhold = List.of(normaltArbeidsforhold, arbeidsforholdZeroProsent, nyttArbeidsforholdFraIM);


        AvklarArbeidsforholdDto avklarArbeidsforholdDto = new AvklarArbeidsforholdDto("Har lagt til et nytt arbeidsforhold", nyeArbeidsforhold);

        //Act
        oppdaterer.oppdater(avklarArbeidsforholdDto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).build(), avklarArbeidsforholdDto.getBegrunnelse()));

        // Assert
        InntektArbeidYtelseGrunnlag grunnlag = hentGrunnlag(behandling);
        List<ArbeidsforholdOverstyring> overstyring = grunnlag.getArbeidsforholdInformasjon()
            .map(ArbeidsforholdInformasjon::getOverstyringer).orElse(Collections.emptyList());

        assertThat(overstyring).hasSize(3);

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(behandling.getAktørId())).før(stp);
        Collection<Yrkesaktivitet> yrkesaktiviteter = filter.getYrkesaktiviteter();
        assertThat(yrkesaktiviteter).hasSize(3);
        assertThat(yrkesaktiviteter.stream().filter(y -> y.getArbeidsgiver().getOrgnr().equals(NAV_ORGNR)).count()).isEqualTo(1);
        assertThat(yrkesaktiviteter.stream().filter(y -> y.getArbeidsgiver().getOrgnr().equals(navikt)).count()).isEqualTo(1);
        var annet = yrkesaktiviteter.stream().filter(y -> y.getArbeidsgiver().getOrgnr().equals(annetforetak)).findFirst().orElse(null);
        var ansattTil = filter.getAnsettelsesPerioder(annet).stream().findFirst()
            .map(AktivitetsAvtale::getPeriode).map(DatoIntervallEntitet::getTomDato).orElse(null);
        assertThat(ansattTil).isEqualTo(stp.minusMonths(2));
    }

    @Test
    public void skal_utlede_handling_lik_SLÅTT_SAMMEN_MED_ANNET() {
        // Arrange
        String navn = "Arbeidsgiver";
        String orgNr = NAV_ORGNR;
        String nyArbeidsforholdRef = InternArbeidsforholdRef.nyRef().getReferanse();
        LocalDate stpDato = LocalDate.of(2019, 1, 1);
        LocalDate fomDato = stpDato.minusYears(3);
        BigDecimal stillingsprosent = BigDecimal.valueOf(100);
        opprettVirksomhetAG();

        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        opprettIAYAggregat(behandling, true, LocalDate.of(2018, 1, 1));

        Aksjonspunkt aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);
        String erstatterArbeidsforholdId = ARBEIDSFORHOLD_REF.getReferanse();

        ArbeidsforholdDto arbeidsforhold = new ArbeidsforholdDto();
        arbeidsforhold.setNavn(navn);
        arbeidsforhold.setFomDato(fomDato);
        arbeidsforhold.setStillingsprosent(stillingsprosent);
        arbeidsforhold.setLagtTilAvSaksbehandler(false);
        arbeidsforhold.setId(erstatterArbeidsforholdId);
        arbeidsforhold.setArbeidsgiverIdentifikator(orgNr);
        arbeidsforhold.setBrukArbeidsforholdet(true);
        arbeidsforhold.setArbeidsforholdId(ARBEIDSFORHOLD_REF.getReferanse());

        ArbeidsforholdDto arbeidsforhold2 = new ArbeidsforholdDto();
        arbeidsforhold2.setNavn(navn);
        arbeidsforhold2.setFomDato(fomDato);
        arbeidsforhold2.setStillingsprosent(stillingsprosent);
        arbeidsforhold2.setLagtTilAvSaksbehandler(false);
        arbeidsforhold2.setId(randomId);
        arbeidsforhold2.setArbeidsgiverIdentifikator(orgNr);
        arbeidsforhold2.setBrukArbeidsforholdet(true);
        arbeidsforhold2.setErstatterArbeidsforholdId(erstatterArbeidsforholdId);
        arbeidsforhold2.setArbeidsforholdId(nyArbeidsforholdRef);

        List<ArbeidsforholdDto> nyeArbeidsforhold = List.of(arbeidsforhold, arbeidsforhold2);
        AvklarArbeidsforholdDto avklarArbeidsforholdDto = new AvklarArbeidsforholdDto("Har lagt til et nytt arbeidsforhold", nyeArbeidsforhold);

        Skjæringstidspunkt stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(stpDato)
            .build();

        //Act
        oppdaterer.oppdater(avklarArbeidsforholdDto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, stp, avklarArbeidsforholdDto.getBegrunnelse()));

        // Assert
        List<ArbeidsforholdOverstyring> overstyringer = hentGrunnlag(behandling)
            .getArbeidsforholdInformasjon()
            .map(ArbeidsforholdInformasjon::getOverstyringer)
            .orElse(Collections.emptyList());

        assertThat(overstyringer).hasSize(2);
        assertThat(overstyringer).anySatisfy(overstyring -> {
            assertThat(overstyring.getHandling()).isEqualTo(ArbeidsforholdHandlingType.SLÅTT_SAMMEN_MED_ANNET);
            assertThat(overstyring.getArbeidsgiver().getOrgnr()).isEqualTo(orgNr);
            assertThat(overstyring.getArbeidsforholdRef().getReferanse()).isEqualTo(ARBEIDSFORHOLD_REF.getReferanse());
            assertThat(overstyring.getNyArbeidsforholdRef().getReferanse()).isEqualTo(nyArbeidsforholdRef);
        });
        assertThat(overstyringer).anySatisfy(overstyring -> {
            assertThat(overstyring.getHandling()).isEqualTo(ArbeidsforholdHandlingType.BRUK);
            assertThat(overstyring.getArbeidsgiver().getOrgnr()).isEqualTo(orgNr);
            assertThat(overstyring.getArbeidsforholdRef().getReferanse()).isEqualTo(nyArbeidsforholdRef);
            assertThat(overstyring.getNyArbeidsforholdRef()).isNull();
        });
    }

    @Test
    public void skal_utlede_handling_lik_inntekt_ikke_med_i_beregningsgrunnlag() {

        // Arrange
        String navn = "Utlandet";
        LocalDate stpDato = LocalDate.of(2019, 1, 1);
        LocalDate fomDato = stpDato.minusYears(3);
        BigDecimal stillingsprosent = BigDecimal.valueOf(100);
        opprettVirksomhetAG();

        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        opprettIAYAggregat(behandling, false, LocalDate.of(2018, 1, 1));

        Aksjonspunkt aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);

        ArbeidsforholdDto arbeidsforhold = new ArbeidsforholdDto();
        arbeidsforhold.setNavn(navn);
        arbeidsforhold.setFomDato(fomDato);
        arbeidsforhold.setStillingsprosent(stillingsprosent);
        arbeidsforhold.setLagtTilAvSaksbehandler(false);
        arbeidsforhold.setId(randomId);
        arbeidsforhold.setArbeidsgiverIdentifikator(NAV_ORGNR);
        arbeidsforhold.setInntektMedTilBeregningsgrunnlag(false);
        arbeidsforhold.setBrukArbeidsforholdet(true);

        List<ArbeidsforholdDto> nyeArbeidsforhold = List.of(arbeidsforhold);
        AvklarArbeidsforholdDto avklarArbeidsforholdDto = new AvklarArbeidsforholdDto("inntekt ikke med til bg", nyeArbeidsforhold);

        Skjæringstidspunkt stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(stpDato)
            .build();

        AksjonspunktOppdaterParameter params = new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, stp, avklarArbeidsforholdDto.getBegrunnelse());

        //Act
        oppdaterer.oppdater(avklarArbeidsforholdDto, params);

        // Assert
        List<ArbeidsforholdOverstyring> overstyring = hentGrunnlag(behandling)
            .getArbeidsforholdInformasjon()
            .map(ArbeidsforholdInformasjon::getOverstyringer)
            .orElse(Collections.emptyList());

        assertThat(overstyring).hasSize(1);
        ArbeidsforholdOverstyring overstyrtArbeidsforhold = overstyring.get(0);
        assertThat(overstyrtArbeidsforhold.getHandling()).isEqualTo(ArbeidsforholdHandlingType.INNTEKT_IKKE_MED_I_BG);
    }

    @Test
    public void skal_utlede_handling_lik_lagt_til_av_saksbehandler() {

        // Arrange
        String navn = "Utlandet";
        String arbeidsforholdId = InternArbeidsforholdRef.nyRef().getReferanse();
        LocalDate stpDato = LocalDate.of(2019, 1, 1);
        LocalDate fomDato = stpDato.minusYears(3);
        BigDecimal stillingsprosent = BigDecimal.valueOf(100);

        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagre(repositoryProvider);
        opprettIAYAggregat(behandling, false, LocalDate.of(2018, 1, 1));

        Aksjonspunkt aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);

        ArbeidsforholdDto arbeidsforhold = new ArbeidsforholdDto();
        arbeidsforhold.setNavn(navn);
        arbeidsforhold.setFomDato(fomDato);
        arbeidsforhold.setStillingsprosent(stillingsprosent);
        arbeidsforhold.setLagtTilAvSaksbehandler(true);
        arbeidsforhold.setArbeidsforholdId(arbeidsforholdId);
        arbeidsforhold.setArbeidsgiverIdentifikator(NAV_ORGNR);
        arbeidsforhold.setBrukArbeidsforholdet(true);

        List<ArbeidsforholdDto> nyeArbeidsforhold = List.of(arbeidsforhold);
        AvklarArbeidsforholdDto avklarArbeidsforholdDto = new AvklarArbeidsforholdDto("Har lagt til et nytt arbeidsforhold", nyeArbeidsforhold);

        Skjæringstidspunkt stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(stpDato)
            .build();

        AksjonspunktOppdaterParameter params = new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, stp, avklarArbeidsforholdDto.getBegrunnelse());

        //Act
        oppdaterer.oppdater(avklarArbeidsforholdDto, params);

        // Assert
        List<ArbeidsforholdOverstyring> overstyring = hentGrunnlag(behandling)
            .getArbeidsforholdInformasjon()
            .map(ArbeidsforholdInformasjon::getOverstyringer)
            .orElse(Collections.emptyList());

        assertThat(overstyring).hasSize(1);
        ArbeidsforholdOverstyring overstyrtArbeidsforhold = overstyring.get(0);
        assertThat(overstyrtArbeidsforhold.getHandling()).isEqualTo(ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER);
    }

    private InntektArbeidYtelseGrunnlag hentGrunnlag(Behandling behandling) {
        return iayTjeneste.finnGrunnlag(behandling.getId()).orElseThrow();
    }

    private void opprettIAYAggregat(Behandling behandling, boolean medArbeidsforholdRef, LocalDate fom) {
        LocalDate tom = AbstractLocalDateInterval.TIDENES_ENDE;
        YrkesaktivitetBuilder yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();
        AktivitetsAvtaleBuilder aktivitetsAvtale = aktivitetsAvtaleBuilder
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medProsentsats(BigDecimal.valueOf(100));
        AktivitetsAvtaleBuilder ansettelsesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        yrkesaktivitetBuilder
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(NAV_ORGNR))
            .medArbeidsforholdId(medArbeidsforholdRef ? ARBEIDSFORHOLD_REF : null)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode);
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        builder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }

    private void opprettTomtIAYAggregat(Behandling behandling) {
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        builder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }

    private void opprettIAYAggregatProdCase(Behandling behandling, boolean medArbeidsforholdRef, LocalDate fom) {
        LocalDate tom = AbstractLocalDateInterval.TIDENES_ENDE;
        YrkesaktivitetBuilder yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medProsentsats(BigDecimal.valueOf(100));
        AktivitetsAvtaleBuilder ansettelsesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        yrkesaktivitetBuilder
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(NAV_ORGNR))
            .medArbeidsforholdId(medArbeidsforholdRef ? ARBEIDSFORHOLD_REF : null)
            .leggTilAktivitetsAvtale(aktivitetsAvtaleBuilder)
            .leggTilAktivitetsAvtale(ansettelsesperiode);
        YrkesaktivitetBuilder yrkesaktivitetBuilder2 = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder2 = yrkesaktivitetBuilder2.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medProsentsats(BigDecimal.ZERO)
            .medSisteLønnsendringsdato(fom.minusMonths(1));
        AktivitetsAvtaleBuilder ansettelsesperiode2 = yrkesaktivitetBuilder2.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        yrkesaktivitetBuilder2
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("973861778"))
            .medArbeidsforholdId(medArbeidsforholdRef ? ARBEIDSFORHOLD_REF : null)
            .leggTilAktivitetsAvtale(aktivitetsAvtaleBuilder2)
            .leggTilAktivitetsAvtale(ansettelsesperiode2);
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder2);
        builder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }

    private void opprettVirksomhetAG() {
        Virksomhet virksomhet = new Virksomhet.Builder()
            .medOrgnr(NAV_ORGNR)
            .build();
        when(virksomhetTjeneste.finnOrganisasjon(any())).thenReturn(Optional.of(virksomhet));
        when(virksomhetTjeneste.hentOrganisasjon(any())).thenReturn(virksomhet);
    }

}
