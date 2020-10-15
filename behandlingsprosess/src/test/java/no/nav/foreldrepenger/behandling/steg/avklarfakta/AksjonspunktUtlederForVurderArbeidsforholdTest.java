package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD;
import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.fp.InntektsmeldingFilterYtelseImpl;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.PåkrevdeInntektsmeldingerTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsmeldingInnsendingsårsak;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class AksjonspunktUtlederForVurderArbeidsforholdTest extends EntityManagerAwareTest {

    private static final String ORGNR = KUNSTIG_ORG;

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    private AksjonspunktUtlederForVurderArbeidsforhold utleder;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);
        var inntektsmeldingArkivTjeneste = new InntektsmeldingRegisterTjeneste(iayTjeneste, inntektsmeldingTjeneste,
            null, new UnitTestLookupInstanceImpl<>(new InntektsmeldingFilterYtelseImpl()));
        var behandlingRepository = new BehandlingRepository(entityManager);
        var søknadRepository = new SøknadRepository(entityManager, behandlingRepository);
        var påkrevdeInntektsmeldingerTjeneste = new PåkrevdeInntektsmeldingerTjeneste(inntektsmeldingArkivTjeneste, søknadRepository);
        var vurderArbeidsforholdTjeneste = new VurderArbeidsforholdTjeneste(påkrevdeInntektsmeldingerTjeneste);
        utleder = new AksjonspunktUtlederForVurderArbeidsforhold(behandlingRepository, iayTjeneste, vurderArbeidsforholdTjeneste);
    }

    @Test
    public void skal_få_aksjonspunkt_når_det_finnes_inntekt_og_ikke_arbeidsforhold() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId1);
        Behandling behandling = lagre(scenario);

        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        opprettInntekt(aktørId1, behandling, ORGNR, arbeidsforholdId);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
    }

    private Behandling lagre(ScenarioMorSøkerForeldrepenger scenario) {
        return scenario.lagre(new BehandlingRepositoryProvider(getEntityManager()));
    }

    private AksjonspunktUtlederInput lagRef(Behandling behandling) {
        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, skjæringstidspunkt));
    }

    @Test
    public void skal_få_aksjonspunkt_når_det_ikke_finnes_inntekt_eller_arbeidsforhold() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId1);
        Behandling behandling = lagre(scenario);
        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).containsExactly(AksjonspunktResultat.opprettForAksjonspunkt(VURDER_ARBEIDSFORHOLD));
    }

    @Test
    public void skal_få_aksjonspunkt_når_mottatt_inntektsmelding_men_ikke_arbeidsforhold() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId1);

        Behandling behandling = lagre(scenario);

        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        sendInnInntektsmeldingPå(behandling, ORGNR, arbeidsforholdId);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isNotEmpty();
    }

    @Test
    public void skal_ikke_få_aksjonspunkt_ved_komplett_søknad_med_inntektsmeldinger() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId1);
        Behandling behandling = lagre(scenario);

        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();

        final InntektArbeidYtelseAggregatBuilder builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        leggTilArbeidsforholdPåBehandling(behandling, ORGNR, arbeidsforholdId, builder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        sendInnInntektsmeldingPå(behandling, ORGNR, arbeidsforholdId);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void skal_ikke_få_aksjonspunkt_ved_søknad_uten_inntekter() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId1);
        Behandling behandling = lagre(scenario);

        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();

        final InntektArbeidYtelseAggregatBuilder builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        leggTilArbeidsforholdPåBehandling(behandling, ORGNR, arbeidsforholdId, builder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isNotEmpty(); // TODO: Expect empty hvis man ikke venter AP når det ikke foreligger inntekt

        // Arrange + Act
        opprettInntekt(aktørId1, behandling, ORGNR, arbeidsforholdId);
        aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isNotEmpty();
    }

    @Test
    public void skal_ikke_få_aksjonspunkt_ved_komplett_søknad_med_to_arbeidsforhold_etter_begge_inntektsmeldinger() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId1);
        Behandling behandling = lagre(scenario);
        String virksomhetOrgnr2 = "100000001";
        var arbeidsforholdId1 = InternArbeidsforholdRef.nyRef();
        var arbeidsforholdId2 = InternArbeidsforholdRef.nyRef();

        final InntektArbeidYtelseAggregatBuilder builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        leggTilArbeidsforholdPåBehandling(behandling, ORGNR, arbeidsforholdId1, builder);
        leggTilArbeidsforholdPåBehandling(behandling, virksomhetOrgnr2, arbeidsforholdId2, builder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        opprettInntekt(aktørId1, behandling, ORGNR, arbeidsforholdId1);
        opprettInntekt(aktørId1, behandling, virksomhetOrgnr2, arbeidsforholdId2);

        sendInnInntektsmeldingPå(behandling, ORGNR, arbeidsforholdId1);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));
        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);

        // Arrange
        sendInnInntektsmeldingPå(behandling, virksomhetOrgnr2, arbeidsforholdId2);
        // Act
        aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));
        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void skal_ikke_få_aksjonspunkt_på_berørt_behandling() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId1);
        Behandling originalBehandling = lagre(scenario);
        scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId1);
        scenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.BERØRT_BEHANDLING);
        Behandling behandling = lagre(scenario);
        String virksomhetOrgnr2 = "100000001";
        var arbeidsforholdId1 = InternArbeidsforholdRef.nyRef();
        var arbeidsforholdId2 = InternArbeidsforholdRef.nyRef();

        final InntektArbeidYtelseAggregatBuilder builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        leggTilArbeidsforholdPåBehandling(behandling, ORGNR, arbeidsforholdId1, builder);
        leggTilArbeidsforholdPåBehandling(behandling, virksomhetOrgnr2, arbeidsforholdId2, builder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        opprettInntekt(aktørId1, behandling, ORGNR, arbeidsforholdId1);
        opprettInntekt(aktørId1, behandling, virksomhetOrgnr2, arbeidsforholdId2);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void skal_ikke_få_aksjonspunkt_ved_endring_i_antall_arbeidsforhold_og_komplett() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId1);
        scenario.medBehandlingStegStart(BehandlingStegType.KONTROLLER_FAKTA);

        Behandling behandling = lagre(scenario);

        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();

        final InntektArbeidYtelseAggregatBuilder builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        leggTilArbeidsforholdPåBehandling(behandling, ORGNR, arbeidsforholdId, builder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        final InntektArbeidYtelseAggregatBuilder builder2 = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        leggTilArbeidsforholdPåBehandling(behandling, ORGNR, arbeidsforholdId, builder2);
        var arbeidsforholdId1 = InternArbeidsforholdRef.nyRef();
        leggTilArbeidsforholdPåBehandling(behandling, ORGNR, arbeidsforholdId1, builder2);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder2);

        sendInnInntektsmeldingPå(behandling, ORGNR, arbeidsforholdId);
        sendInnInntektsmeldingPå(behandling, ORGNR, arbeidsforholdId1); // Kommer inntektsmelding på arbeidsforhold vi ikke kjenner før STP

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    private void leggTilArbeidsforholdPåBehandling(Behandling behandling, String virksomhetOrgnr, InternArbeidsforholdRef ref,
                                                   InntektArbeidYtelseAggregatBuilder builder) {
        final Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(virksomhetOrgnr);
        final InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder arbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        final Opptjeningsnøkkel nøkkel = Opptjeningsnøkkel.forArbeidsforholdIdMedArbeidgiver(ref, arbeidsgiver);
        final YrkesaktivitetBuilder yrkesaktivitetBuilderForType = arbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(nøkkel,
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktivitetBuilderForType
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(ref)
            .leggTilAktivitetsAvtale(yrkesaktivitetBuilderForType
                .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMed(LocalDate.now().minusMonths(3)), false)
                .medSisteLønnsendringsdato(LocalDate.now().minusMonths(3))
                .medProsentsats(BigDecimal.valueOf(100)))
            .leggTilAktivitetsAvtale(yrkesaktivitetBuilderForType
                .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMed(LocalDate.now().minusMonths(3)), true));
        arbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilderForType);
        builder.leggTilAktørArbeid(arbeidBuilder);
    }

    private void sendInnInntektsmeldingPå(Behandling behandling, String virksomhetOrgnr, InternArbeidsforholdRef arbeidsforholdId) {
        MottattDokument mottattDokument = new MottattDokument.Builder()
            .medDokumentType(DokumentTypeId.INNTEKTSMELDING)
            .medFagsakId(behandling.getFagsakId())
            .medMottattDato(LocalDate.now())
            .medBehandlingId(behandling.getId())
            .medElektroniskRegistrert(true)
            .medJournalPostId(new JournalpostId("2"))
            .build();
        new MottatteDokumentRepository(getEntityManager()).lagre(mottattDokument);
        final InntektsmeldingBuilder inntektsmeldingBuilder = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet(virksomhetOrgnr))
            .medInnsendingstidspunkt(LocalDateTime.now())
            .medArbeidsforholdId(arbeidsforholdId)
            .medJournalpostId(mottattDokument.getJournalpostId())
            .medBeløp(BigDecimal.TEN)
            .medStartDatoPermisjon(LocalDate.now())
            .medNærRelasjon(false)
            .medInntektsmeldingaarsak(InntektsmeldingInnsendingsårsak.NY);
        inntektsmeldingTjeneste.lagreInntektsmelding(behandling.getFagsak().getSaksnummer(), behandling.getId(), inntektsmeldingBuilder);
    }

    private void opprettInntekt(AktørId aktørId1, Behandling behandling, String virksomhetOrgnr, InternArbeidsforholdRef arbeidsforholdRef) {
        InntektArbeidYtelseAggregatBuilder builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder inntektBuilder = builder.getAktørInntektBuilder(aktørId1);
        final Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(virksomhetOrgnr);
        final Opptjeningsnøkkel opptjeningsnøkkel = Opptjeningsnøkkel.forArbeidsforholdIdMedArbeidgiver(arbeidsforholdRef, arbeidsgiver);
        InntektBuilder tilInntektspost = inntektBuilder.getInntektBuilder(InntektsKilde.INNTEKT_OPPTJENING, opptjeningsnøkkel);
        tilInntektspost.medArbeidsgiver(arbeidsgiver);
        InntektspostBuilder inntektspostBuilder = tilInntektspost.getInntektspostBuilder();

        InntektspostBuilder inntektspost = inntektspostBuilder
            .medBeløp(BigDecimal.TEN)
            .medPeriode(LocalDate.now().minusMonths(1), LocalDate.now())
            .medInntektspostType(InntektspostType.LØNN);

        tilInntektspost
            .leggTilInntektspost(inntektspost)
            .medInntektsKilde(InntektsKilde.INNTEKT_OPPTJENING);

        InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder aktørInntekt = inntektBuilder
            .leggTilInntekt(tilInntektspost);

        builder.leggTilAktørInntekt(aktørInntekt);

        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }
}
