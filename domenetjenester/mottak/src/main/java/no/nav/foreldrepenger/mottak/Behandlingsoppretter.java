package no.nav.foreldrepenger.mottak;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingOpprettingTjeneste;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.HenleggBehandlingTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.MottattDokumentPersisterer;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

@Dependent
public class Behandlingsoppretter {

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingOpprettingTjeneste behandlingOpprettingTjeneste;
    private MottattDokumentPersisterer mottattDokumentPersisterer;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BehandlingRevurderingTjeneste behandlingRevurderingTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;
    private SøknadRepository søknadRepository;

    public Behandlingsoppretter() {
        // For CDI
    }

    @Inject
    public Behandlingsoppretter(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                BehandlingOpprettingTjeneste behandlingOpprettingTjeneste,
                                MottattDokumentPersisterer mottattDokumentPersisterer,
                                MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                BehandlingRevurderingTjeneste behandlingRevurderingTjeneste,
                                HenleggBehandlingTjeneste henleggBehandlingTjeneste) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingOpprettingTjeneste = behandlingOpprettingTjeneste;
        this.mottattDokumentPersisterer = mottattDokumentPersisterer;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.behandlingRevurderingTjeneste = behandlingRevurderingTjeneste;
        this.behandlingsresultatRepository = behandlingRepositoryProvider.getBehandlingsresultatRepository();
        this.behandlingVedtakRepository = behandlingRepositoryProvider.getBehandlingVedtakRepository();
        this.søknadRepository = behandlingRepositoryProvider.getSøknadRepository();
        this.henleggBehandlingTjeneste = henleggBehandlingTjeneste;
    }

    /**
     * Opprett og Oppdater under vil opprette behandling og kopiere grunnlag, men ikke opprette start/fortsett tasks.
     */
    public Behandling opprettFørstegangsbehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Optional<Behandling> tidligereBehandling) {
        if (!tidligereBehandling.map(Behandling::erSaksbehandlingAvsluttet).orElse(true)) {
            throw new IllegalStateException("Utviklerfeil: Prøver opprette ny behandling når det finnes åpen av samme type: " + fagsak.getId());
        }
        return behandlingOpprettingTjeneste.opprettBehandlingUtenHistorikk(fagsak, BehandlingType.FØRSTEGANGSSØKNAD, behandlingÅrsakType);
    }

    public Behandling opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Behandling forrigeBehandling, boolean kopierGrunnlag) {
        var nyFørstegangsbehandling = opprettFørstegangsbehandling(fagsak, behandlingÅrsakType, Optional.ofNullable(forrigeBehandling));
        if (forrigeBehandling != null && kopierGrunnlag) {
            kopierTidligereGrunnlagFraTil(fagsak, forrigeBehandling, nyFørstegangsbehandling);
        }
        opprettInntektsmeldingerFraMottatteDokumentPåNyBehandling(nyFørstegangsbehandling);
        if (forrigeBehandling != null)
            kopierVedlegg(forrigeBehandling, nyFørstegangsbehandling);
        return nyFørstegangsbehandling;
    }

    public Behandling opprettRevurdering(Fagsak fagsak, BehandlingÅrsakType revurderingsÅrsak) {
        var revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        return revurderingTjeneste.opprettAutomatiskRevurdering(fagsak, revurderingsÅrsak, behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak));
    }

    public Behandling opprettManuellRevurdering(Fagsak fagsak, BehandlingÅrsakType revurderingsÅrsak) {
        var revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        return revurderingTjeneste.opprettManuellRevurdering(fagsak, revurderingsÅrsak, behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak));
    }

    public Behandling oppdaterBehandlingViaHenleggelse(Behandling sisteYtelseBehandling) {
        var årsakstype = sisteYtelseBehandling.getBehandlingÅrsaker().stream()
            .map(BehandlingÅrsak::getBehandlingÅrsakType)
            .findFirst().orElse(BehandlingÅrsakType.UDEFINERT);
        return oppdaterBehandlingViaHenleggelse(sisteYtelseBehandling, årsakstype);
    }

    public Behandling oppdaterBehandlingViaHenleggelse(Behandling sisteYtelseBehandling, BehandlingÅrsakType revurderingsÅrsak) {
        // Ifm køhåndtering - kun relevant for Foreldrepenger. REGSØK har relevant logikk for FØRSTEGANG.
        // Må håndtere revurderinger med åpent aksjonspunkt: Kopier med siste papirsøknad hvis finnes så AP reutledes i REGSØK
        var uregistrertPapirSøknadFP = sisteYtelseBehandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER);
        henleggBehandling(sisteYtelseBehandling);
        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(sisteYtelseBehandling.getType())) {
            return opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(sisteYtelseBehandling.getFagsak(), revurderingsÅrsak, sisteYtelseBehandling,false);
        }
        var revurdering = opprettRevurdering(sisteYtelseBehandling.getFagsak(), revurderingsÅrsak);

        if (uregistrertPapirSøknadFP) {
            kopierPapirsøknadVedBehov(sisteYtelseBehandling, revurdering);
        }
        opprettInntektsmeldingerFraMottatteDokumentPåNyBehandling(revurdering);
        kopierVedlegg(sisteYtelseBehandling, revurdering);

        // Kopier behandlingsårsaker fra forrige behandling
        var forrigeÅrsaker = sisteYtelseBehandling.getBehandlingÅrsaker().stream()
            .map(BehandlingÅrsak::getBehandlingÅrsakType)
            .filter(bat -> !revurderingsÅrsak.equals(bat))
            .toList();
        if (!forrigeÅrsaker.isEmpty()) {
            var årsakBuilder = BehandlingÅrsak.builder(forrigeÅrsaker);
            revurdering.getOriginalBehandlingId().ifPresent(årsakBuilder::medOriginalBehandlingId);
            årsakBuilder.medManueltOpprettet(sisteYtelseBehandling.erManueltOpprettet()).buildFor(revurdering);
        }

        var nyLås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, nyLås);

        return revurdering;
    }

    public void henleggBehandling(Behandling behandling) {
        var lås = behandlingRepository.taSkriveLås(behandling);
        henleggBehandlingTjeneste.henleggBehandlingTeknisk(behandling, lås, BehandlingResultatType.MERGET_OG_HENLAGT, "Mottatt ny søknad");
    }

    public void opprettInntektsmeldingerFraMottatteDokumentPåNyBehandling(Behandling nyBehandling) {
            hentAlleInntektsmeldingdokumenter(nyBehandling.getFagsakId()).stream()
                .sorted(MottattDokumentSorterer.sorterMottattDokument())
                .forEach(mottattDokument ->
                    mottattDokumentPersisterer.persisterDokumentinnhold(mottattDokument, nyBehandling));

    }

    public void leggTilBehandlingsårsak(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        if (behandlingÅrsakType == null || BehandlingÅrsakType.UDEFINERT.equals(behandlingÅrsakType)) return;
        if (!behandling.harBehandlingÅrsak(behandlingÅrsakType)) {
            var builder = BehandlingÅrsak.builder(behandlingÅrsakType);
            behandling.getOriginalBehandlingId().ifPresent(builder::medOriginalBehandlingId);
            builder.buildFor(behandling);

            var behandlingLås = behandlingRepository.taSkriveLås(behandling);
            behandlingRepository.lagre(behandling, behandlingLås);
        }
    }

    private void kopierPapirsøknadVedBehov(Behandling opprinneligBehandling, Behandling nyBehandling) {
        var søknad = mottatteDokumentTjeneste.hentMottatteDokumentFagsak(opprinneligBehandling.getFagsakId()).stream()
            .filter(MottattDokument::erSøknadsDokument)
            .max(Comparator.comparing(MottattDokument::getOpprettetTidspunkt))
            .filter(MottattDokument::erUstrukturertDokument);

        søknad.ifPresent(s -> {
            var dokument = new MottattDokument.Builder(s)
                .medBehandlingId(nyBehandling.getId())
                .build();
            mottatteDokumentTjeneste.lagreMottattDokumentPåFagsak(dokument);
        });
    }

    private void kopierVedlegg(Behandling opprinneligBehandling, Behandling nyBehandling) {
        var vedlegg = mottatteDokumentTjeneste.hentMottatteDokumentVedlegg(opprinneligBehandling.getId());

        if (!vedlegg.isEmpty()) {
            vedlegg.forEach(vedlegget -> {
                var dokument = new MottattDokument.Builder(vedlegget)
                    .medBehandlingId(nyBehandling.getId())
                    .build();
                mottatteDokumentTjeneste.lagreMottattDokumentPåFagsak(dokument);
            });
        }
    }

    private List<MottattDokument> hentAlleInntektsmeldingdokumenter(Long fagsakId) {
        return mottatteDokumentTjeneste.hentMottatteDokumentFagsak(fagsakId).stream()
            .filter(dok -> DokumentTypeId.INNTEKTSMELDING.equals(dok.getDokumentType()))
            .toList();
    }


    public void settSomKøet(Behandling nyKøetBehandling) {
        behandlingskontrollTjeneste.settBehandlingPåVent(nyKøetBehandling, AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING, null, null, Venteårsak.VENT_ÅPEN_BEHANDLING);
    }

    public boolean erOpphørtBehandling(Behandling behandling) {
        return behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId())
            .map(BehandlingVedtak::getVedtakResultatType)
            .filter(VedtakResultatType.OPPHØR::equals)
            .isPresent();
    }

    public boolean erAvslåttBehandling(Behandling behandling) {
        return behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId())
            .map(BehandlingVedtak::getVedtakResultatType)
            .filter(VedtakResultatType.AVSLAG::equals)
            .isPresent();
    }

    public boolean erUtsattBehandling(Behandling behandling) {
        var forrigeResultatUtsatt = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())
            .map(Behandlingsresultat::getBehandlingResultatType)
            .filter(BehandlingResultatType.FORELDREPENGER_SENERE::equals)
            .isPresent();
        return behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(behandling.getFagsakId()).isEmpty() && forrigeResultatUtsatt;
    }

    private void kopierTidligereGrunnlagFraTil(Fagsak fagsak, Behandling behandlingMedSøknad, Behandling nyBehandling) {
        var søknad = søknadRepository.hentSøknad(behandlingMedSøknad.getId());
        if (søknad != null) {
            søknadRepository.lagreOgFlush(nyBehandling, søknad);
        }
        var revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        revurderingTjeneste.kopierAlleGrunnlagFraTidligereBehandling(behandlingMedSøknad, nyBehandling);
    }

    public void kopierAlleGrunnlagFraTidligereBehandlingTilUtsattSøknad(Fagsak fagsak, Behandling forrige, Behandling nyBehandling) {
        var revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        revurderingTjeneste.kopierAlleGrunnlagFraTidligereBehandlingTilUtsattSøknad(forrige, nyBehandling);
    }

    public Behandling opprettNyFørstegangsbehandlingFraTidligereSøknad(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Behandling behandlingMedSøknad) {
        var sisteYtelsesbehandling = behandlingRevurderingTjeneste.hentAktivIkkeBerørtEllerSisteYtelsesbehandling(fagsak.getId()).orElseThrow();
        var harÅpenBehandling = !sisteYtelsesbehandling.erSaksbehandlingAvsluttet();
        var behandling = harÅpenBehandling ? oppdaterBehandlingViaHenleggelse(sisteYtelsesbehandling, behandlingÅrsakType)
            : opprettFørstegangsbehandling(fagsak, behandlingÅrsakType, Optional.of(behandlingMedSøknad));

        kopierTidligereGrunnlagFraTil(fagsak, behandlingMedSøknad, behandling);
        return behandling;
    }

    public boolean erBehandlingOgFørstegangsbehandlingHenlagt(Fagsak fagsak) {
        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        var behandlingsresultat = behandling.flatMap(b -> behandlingsresultatRepository.hentHvisEksisterer(b.getId()));
        if (behandlingsresultat.isPresent() && erHenlagt(behandlingsresultat.get())) {
            var førstegangsbehandlingBehandlingsresultat = hentFørstegangsbehandlingsresultat(fagsak);
            return førstegangsbehandlingBehandlingsresultat.map(this::erHenlagt).orElse(false);
        }
        return false;
    }

    private Optional<Behandlingsresultat> hentFørstegangsbehandlingsresultat(Fagsak fagsak) {
        var førstegangsbehandling = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(
            fagsak.getId(), BehandlingType.FØRSTEGANGSSØKNAD);
        return førstegangsbehandling.flatMap(b -> behandlingsresultatRepository.hentHvisEksisterer(b.getId()));
    }

    private boolean erHenlagt(Behandlingsresultat br) {
        //Sjekker andre koder enn Behandlingsresultat.isBehandlingHenlagt()
        return BehandlingResultatType.getHenleggelseskoderForSøknad().contains(br.getBehandlingResultatType());
    }
}
