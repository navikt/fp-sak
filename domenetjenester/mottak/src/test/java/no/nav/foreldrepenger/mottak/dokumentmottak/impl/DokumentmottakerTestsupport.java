package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static java.time.LocalDate.now;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType.FØRSTEGANGSSØKNAD;

import java.time.LocalDate;

import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;

@CdiDbAwareTest
public abstract class DokumentmottakerTestsupport {

    protected static final int FRIST_INNSENDING_UKER = 6;
    protected final LocalDate DATO_ETTER_INNSENDINGSFRISTEN = LocalDate.now().minusWeeks(FRIST_INNSENDING_UKER + 2);
    protected final LocalDate DATO_FØR_INNSENDINGSFRISTEN = LocalDate.now().minusWeeks(FRIST_INNSENDING_UKER - 2);

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
            var scenario = ScenarioMorSøkerEngangsstønad.forFødsel()
                .medBehandlingType(FØRSTEGANGSSØKNAD);
            behandling = scenario.lagre(repositoryProvider);
        }

        if (FagsakYtelseType.FORELDREPENGER.equals(fagsakYtelseType)) {
            var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(FØRSTEGANGSSØKNAD);
            behandling = scenario.lagre(repositoryProvider);
        }

        return behandling;
    }

    protected Behandling opprettBehandling(FagsakYtelseType fagsakYtelseType, BehandlingType behandlingType, BehandlingResultatType behandlingResultatType, Avslagsårsak avslagsårsak, VedtakResultatType vedtakResultatType, LocalDate vedtaksdato) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(fagsakYtelseType)) {
            var scenario = ScenarioMorSøkerEngangsstønad.forFødsel()
                .medBehandlingType(behandlingType);
            return opprettBehandling(scenario, behandlingResultatType, avslagsårsak, vedtakResultatType, vedtaksdato);
        }

        if (FagsakYtelseType.FORELDREPENGER.equals(fagsakYtelseType)) {
            var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(behandlingType);
            return opprettBehandling(scenario, behandlingResultatType, avslagsårsak, vedtakResultatType, vedtaksdato);
        }

        throw new UnsupportedOperationException("Fiks testoppsett");
    }

    private Behandling opprettBehandling(AbstractTestScenario<?> scenario, BehandlingResultatType behandlingResultatType, Avslagsårsak avslagsårsak, VedtakResultatType vedtakResultatType, LocalDate vedtaksdato) {

        scenario.medBehandlingsresultat(Behandlingsresultat.builder()
            .medBehandlingResultatType(behandlingResultatType)
            .medAvslagsårsak(avslagsårsak));
        var behandling = scenario.lagre(repositoryProvider);

        var behandlingLås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        repositoryProvider.getBehandlingRepository().lagre(behandling, behandlingLås);

        var originalVedtak = BehandlingVedtak.builder()
            .medVedtakstidspunkt(vedtaksdato.atStartOfDay())
            .medBehandlingsresultat(behandling.getBehandlingsresultat())
            .medVedtakResultatType(vedtakResultatType)
            .medAnsvarligSaksbehandler("fornavn etternavn")
            .build();

        behandling.getFagsak().setAvsluttet();
        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingVedtakRepository().lagre(originalVedtak, behandlingLås);

        var vilkårResultat = VilkårResultat.builder()
            .leggTilVilkårAvslått(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallMerknad.VM_1019)
            .buildFor(behandling);
        repositoryProvider.getBehandlingRepository().lagre(vilkårResultat, behandlingLås);


        return behandling;
    }

    protected MottattDokument dummyInntektsmeldingDokument(Behandling behandling) {
        return DokumentmottakTestUtil.byggMottattDokument(DokumentTypeId.INNTEKTSMELDING, behandling.getFagsakId(), "", now(), true, "123");
    }

    protected MottattDokument dummyVedleggDokument(Behandling behandling) {
        return DokumentmottakTestUtil.byggMottattDokument(DokumentTypeId.LEGEERKLÆRING, behandling.getFagsakId(), "", now(), true, "456");
    }

    protected MottattDokument dummyVedleggDokumentTypeAnnet(Behandling behandling) {
        return DokumentmottakTestUtil.byggMottattDokument(DokumentTypeId.ANNET, behandling.getFagsakId(), "", now(), true, "456");
    }

    protected MottattDokument dummySøknadDokument(Behandling behandling) {
        return DokumentmottakTestUtil.byggMottattDokument(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL, behandling.getFagsakId(), "", now(), true, "456");
    }

}
