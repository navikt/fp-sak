package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static java.time.LocalDate.now;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType.FØRSTEGANGSSØKNAD;

import java.time.LocalDate;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public abstract class DokumentmottakerTestsupport {

    protected static final int FRIST_INNSENDING_UKER = 6;
    protected final LocalDate DATO_ETTER_INNSENDINGSFRISTEN = LocalDate.now().minusWeeks(FRIST_INNSENDING_UKER + 2);
    protected final LocalDate DATO_FØR_INNSENDINGSFRISTEN = LocalDate.now().minusWeeks(FRIST_INNSENDING_UKER - 2);

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    protected DokumentmottakerFelles dokumentmottakerFelles;
    @Inject
    protected MottatteDokumentTjeneste mottatteDokumentTjeneste;
    @Inject
    protected Behandlingsoppretter behandlingsoppretter;
    @Inject
    protected Kompletthetskontroller kompletthetskontroller;
    @Inject
    protected BehandlingRepositoryProvider repositoryProvider;
    @Inject
    protected ForeldrepengerUttakTjeneste fpUttakTjeneste;

    protected Behandling opprettNyBehandlingUtenVedtak(FagsakYtelseType fagsakYtelseType) {
        Behandling behandling = null;

        if (FagsakYtelseType.ENGANGSTØNAD.equals(fagsakYtelseType)) {
            ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel()
                .medBehandlingType(FØRSTEGANGSSØKNAD);
            behandling = scenario.lagre(repositoryProvider);
        }

        if (FagsakYtelseType.FORELDREPENGER.equals(fagsakYtelseType)) {
            ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(FØRSTEGANGSSØKNAD);
            behandling = scenario.lagre(repositoryProvider);
        }

        return behandling;
    }

    protected Behandling opprettBehandling(FagsakYtelseType fagsakYtelseType, BehandlingType behandlingType, BehandlingResultatType behandlingResultatType, Avslagsårsak avslagsårsak, VedtakResultatType vedtakResultatType, LocalDate vedtaksdato) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(fagsakYtelseType)) {
            ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel()
                .medBehandlingType(behandlingType);
            return opprettBehandling(scenario, behandlingResultatType, avslagsårsak, vedtakResultatType, vedtaksdato);
        }

        if (FagsakYtelseType.FORELDREPENGER.equals(fagsakYtelseType)) {
            ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(behandlingType);
            return opprettBehandling(scenario, behandlingResultatType, avslagsårsak, vedtakResultatType, vedtaksdato);
        }

        throw new UnsupportedOperationException("Fiks testoppsett");
    }

    private Behandling opprettBehandling(AbstractTestScenario<?> scenario, BehandlingResultatType behandlingResultatType, Avslagsårsak avslagsårsak, VedtakResultatType vedtakResultatType, LocalDate vedtaksdato) {

        scenario.medBehandlingsresultat(Behandlingsresultat.builder()
            .medBehandlingResultatType(behandlingResultatType)
            .medAvslagsårsak(avslagsårsak));
        Behandling behandling = scenario.lagre(repositoryProvider);

        BehandlingLås behandlingLås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        repositoryProvider.getBehandlingRepository().lagre(behandling, behandlingLås);

        BehandlingVedtak originalVedtak = BehandlingVedtak.builder()
            .medVedtakstidspunkt(vedtaksdato.atStartOfDay())
            .medBehandlingsresultat(behandling.getBehandlingsresultat())
            .medVedtakResultatType(vedtakResultatType)
            .medAnsvarligSaksbehandler("fornavn etternavn")
            .build();

        behandling.getFagsak().setAvsluttet();
        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingVedtakRepository().lagre(originalVedtak, behandlingLås);

        VilkårResultat vilkårResultat = VilkårResultat.builder()
            .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.IKKE_OPPFYLT)
            .buildFor(behandling);
        repositoryProvider.getBehandlingRepository().lagre(vilkårResultat, behandlingLås);


        return behandling;
    }

    protected MottattDokument dummyInntektsmeldingDokument(Behandling behandling) {
        DokumentTypeId dokumentTypeId = DokumentTypeId.INNTEKTSMELDING;
        return DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, behandling.getFagsakId(), "", now(), true, "123");
    }

    protected MottattDokument dummyVedleggDokument(Behandling behandling) {
        DokumentTypeId dokumentTypeId = DokumentTypeId.LEGEERKLÆRING;
        return DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, behandling.getFagsakId(), "", now(), true, "456");
    }

    protected MottattDokument dummyVedleggDokumentTypeAnnet(Behandling behandling) {
        DokumentTypeId dokumentTypeId = DokumentTypeId.ANNET;
        return DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, behandling.getFagsakId(), "", now(), true, "456");
    }

    protected MottattDokument dummySøknadDokument(Behandling behandling) {
        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;
        return DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, behandling.getFagsakId(), "", now(), true, "456");
    }

}
