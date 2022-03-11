package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD;
import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt.AksjonspunktUtlederForVurderArbeidsforhold;
import no.nav.foreldrepenger.domene.arbeidsforhold.fp.InntektsmeldingFilterYtelseImpl;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.PåkrevdeInntektsmeldingerTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsmeldingInnsendingsårsak;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

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
        var aktørId1 = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId1);
        var behandling = lagre(scenario);

        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        opprettInntekt(aktørId1, behandling, ORGNR, arbeidsforholdId);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
    }

    private Behandling lagre(ScenarioMorSøkerForeldrepenger scenario) {
        return scenario.lagre(new BehandlingRepositoryProvider(getEntityManager()));
    }

    private AksjonspunktUtlederInput lagRef(Behandling behandling) {
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, skjæringstidspunkt));
    }

    @Test
    public void skal_få_aksjonspunkt_når_det_ikke_finnes_inntekt_eller_arbeidsforhold() {
        // Arrange
        var aktørId1 = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId1);
        var behandling = lagre(scenario);
        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).containsExactly(AksjonspunktResultat.opprettForAksjonspunkt(VURDER_ARBEIDSFORHOLD));
    }

    @Test
    public void skal_få_aksjonspunkt_når_mottatt_inntektsmelding_men_ikke_arbeidsforhold() {
        // Arrange
        var aktørId1 = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId1);

        var behandling = lagre(scenario);

        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        sendInnInntektsmeldingPå(behandling, ORGNR, arbeidsforholdId);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isNotEmpty();
    }

    @Test
    public void skal_ikke_få_aksjonspunkt_ved_komplett_søknad_med_inntektsmeldinger() {
        // Arrange
        var aktørId1 = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId1);
        var behandling = lagre(scenario);

        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();

        final var builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        leggTilArbeidsforholdPåBehandling(behandling, ORGNR, arbeidsforholdId, builder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        sendInnInntektsmeldingPå(behandling, ORGNR, arbeidsforholdId);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void skal_ikke_få_aksjonspunkt_ved_søknad_uten_inntekter() {
        // Arrange
        var aktørId1 = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId1);
        var behandling = lagre(scenario);

        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();

        final var builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        leggTilArbeidsforholdPåBehandling(behandling, ORGNR, arbeidsforholdId, builder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

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
        var aktørId1 = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId1);
        var behandling = lagre(scenario);
        var virksomhetOrgnr2 = "100000001";
        var arbeidsforholdId1 = InternArbeidsforholdRef.nyRef();
        var arbeidsforholdId2 = InternArbeidsforholdRef.nyRef();

        final var builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        leggTilArbeidsforholdPåBehandling(behandling, ORGNR, arbeidsforholdId1, builder);
        leggTilArbeidsforholdPåBehandling(behandling, virksomhetOrgnr2, arbeidsforholdId2, builder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        opprettInntekt(aktørId1, behandling, ORGNR, arbeidsforholdId1);
        opprettInntekt(aktørId1, behandling, virksomhetOrgnr2, arbeidsforholdId2);

        sendInnInntektsmeldingPå(behandling, ORGNR, arbeidsforholdId1);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));
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
        var aktørId1 = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId1);
        var originalBehandling = lagre(scenario);
        scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId1);
        scenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.BERØRT_BEHANDLING);
        var behandling = lagre(scenario);
        var virksomhetOrgnr2 = "100000001";
        var arbeidsforholdId1 = InternArbeidsforholdRef.nyRef();
        var arbeidsforholdId2 = InternArbeidsforholdRef.nyRef();

        final var builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        leggTilArbeidsforholdPåBehandling(behandling, ORGNR, arbeidsforholdId1, builder);
        leggTilArbeidsforholdPåBehandling(behandling, virksomhetOrgnr2, arbeidsforholdId2, builder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        opprettInntekt(aktørId1, behandling, ORGNR, arbeidsforholdId1);
        opprettInntekt(aktørId1, behandling, virksomhetOrgnr2, arbeidsforholdId2);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void skal_ikke_få_aksjonspunkt_ved_endring_i_antall_arbeidsforhold_og_komplett() {
        // Arrange
        var aktørId1 = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBruker(aktørId1);
        scenario.medBehandlingStegStart(BehandlingStegType.KONTROLLER_FAKTA);

        var behandling = lagre(scenario);

        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();

        final var builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        leggTilArbeidsforholdPåBehandling(behandling, ORGNR, arbeidsforholdId, builder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        final var builder2 = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        leggTilArbeidsforholdPåBehandling(behandling, ORGNR, arbeidsforholdId, builder2);
        var arbeidsforholdId1 = InternArbeidsforholdRef.nyRef();
        leggTilArbeidsforholdPåBehandling(behandling, ORGNR, arbeidsforholdId1, builder2);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder2);

        sendInnInntektsmeldingPå(behandling, ORGNR, arbeidsforholdId);
        sendInnInntektsmeldingPå(behandling, ORGNR, arbeidsforholdId1); // Kommer inntektsmelding på arbeidsforhold vi ikke kjenner før STP

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    private void leggTilArbeidsforholdPåBehandling(Behandling behandling, String virksomhetOrgnr, InternArbeidsforholdRef ref,
            InntektArbeidYtelseAggregatBuilder builder) {
        final var arbeidsgiver = Arbeidsgiver.virksomhet(virksomhetOrgnr);
        final var arbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        final var nøkkel = Opptjeningsnøkkel.forArbeidsforholdIdMedArbeidgiver(ref, arbeidsgiver);
        final var yrkesaktivitetBuilderForType = arbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(nøkkel,
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
        var mottattDokument = new MottattDokument.Builder()
                .medDokumentType(DokumentTypeId.INNTEKTSMELDING)
                .medFagsakId(behandling.getFagsakId())
                .medMottattDato(LocalDate.now())
                .medBehandlingId(behandling.getId())
                .medElektroniskRegistrert(true)
                .medJournalPostId(new JournalpostId("2"))
                .build();
        new MottatteDokumentRepository(getEntityManager()).lagre(mottattDokument);
        final var inntektsmeldingBuilder = InntektsmeldingBuilder.builder()
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
        var builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        var inntektBuilder = builder.getAktørInntektBuilder(aktørId1);
        final var arbeidsgiver = Arbeidsgiver.virksomhet(virksomhetOrgnr);
        final var opptjeningsnøkkel = Opptjeningsnøkkel.forArbeidsforholdIdMedArbeidgiver(arbeidsforholdRef, arbeidsgiver);
        var tilInntektspost = inntektBuilder.getInntektBuilder(InntektsKilde.INNTEKT_OPPTJENING, opptjeningsnøkkel);
        tilInntektspost.medArbeidsgiver(arbeidsgiver);
        var inntektspostBuilder = tilInntektspost.getInntektspostBuilder();

        var inntektspost = inntektspostBuilder
                .medBeløp(BigDecimal.TEN)
                .medPeriode(LocalDate.now().minusMonths(1), LocalDate.now())
                .medInntektspostType(InntektspostType.LØNN);

        tilInntektspost
                .leggTilInntektspost(inntektspost)
                .medInntektsKilde(InntektsKilde.INNTEKT_OPPTJENING);

        var aktørInntekt = inntektBuilder
                .leggTilInntekt(tilInntektspost);

        builder.leggTilAktørInntekt(aktørInntekt);

        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }
}
