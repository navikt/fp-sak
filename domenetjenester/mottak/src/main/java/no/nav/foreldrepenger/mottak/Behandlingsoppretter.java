package no.nav.foreldrepenger.mottak;

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.DokumentPersistererTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

@Dependent
public class Behandlingsoppretter {

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private DokumentPersistererTjeneste dokumentPersistererTjeneste;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BehandlingRevurderingRepository revurderingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private SøknadRepository søknadRepository;

    public Behandlingsoppretter() {
        // For CDI
    }

    @Inject
    public Behandlingsoppretter(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                    BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                    DokumentPersistererTjeneste dokumentPersistererTjeneste,
                                    MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                    BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) { // NOSONAR
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.dokumentPersistererTjeneste = dokumentPersistererTjeneste;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.mottatteDokumentRepository = behandlingRepositoryProvider.getMottatteDokumentRepository();
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.revurderingRepository = behandlingRepositoryProvider.getBehandlingRevurderingRepository();
        this.behandlingsresultatRepository = behandlingRepositoryProvider.getBehandlingsresultatRepository();
        this.søknadRepository = behandlingRepositoryProvider.getSøknadRepository();
    }

    public boolean erKompletthetssjekkPassert(Behandling behandling) {
        return behandlingskontrollTjeneste.erStegPassert(behandling, BehandlingStegType.VURDER_KOMPLETTHET);
    }

    /**
     * Opprett og Oppdater under vil opprette behandling og kopiere grunnlag, men ikke opprette start/fortsett tasks.
     */
    public Behandling opprettFørstegangsbehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Optional<Behandling> tidligereBehandling) {
        BehandlingType behandlingType = BehandlingType.FØRSTEGANGSSØKNAD;
        if (!tidligereBehandling.map(Behandling::erSaksbehandlingAvsluttet).orElse(true)) {
            throw new IllegalStateException("Utviklerfeil: Prøver opprette ny behandling når det finnes åpen av samme type: " + fagsak.getId());
        }
        return behandlingskontrollTjeneste.opprettNyBehandling(fagsak, behandlingType, (beh) -> {
            if (!BehandlingÅrsakType.UDEFINERT.equals(behandlingÅrsakType)) {
                BehandlingÅrsak.builder(behandlingÅrsakType).buildFor(beh);
            }
            beh.setBehandlingstidFrist(LocalDate.now().plusWeeks(behandlingType.getBehandlingstidFristUker()));
            OrganisasjonsEnhet enhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak);
            beh.setBehandlendeEnhet(enhet);
        }); // NOSONAR
    }

    public Behandling opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Behandling forrigeBehandling, boolean kopierGrunnlag) {
        Behandling nyFørstegangsbehandling = opprettFørstegangsbehandling(fagsak, behandlingÅrsakType, Optional.ofNullable(forrigeBehandling));
        if (kopierGrunnlag) {
            kopierTidligereGrunnlagFraTil(fagsak, forrigeBehandling, nyFørstegangsbehandling);
        }
        opprettInntektsmeldingerFraMottatteDokumentPåNyBehandling(nyFørstegangsbehandling);
        kopierVedlegg(forrigeBehandling, nyFørstegangsbehandling);
        return nyFørstegangsbehandling;
    }

    public Behandling opprettRevurdering(Fagsak fagsak, BehandlingÅrsakType revurderingsÅrsak) {
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        return revurderingTjeneste.opprettAutomatiskRevurdering(fagsak, revurderingsÅrsak, behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak));
    }

    public Behandling opprettManuellRevurdering(Fagsak fagsak, BehandlingÅrsakType revurderingsÅrsak) {
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        return revurderingTjeneste.opprettManuellRevurdering(fagsak, revurderingsÅrsak, behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak));
    }

    public Behandling oppdaterBehandlingViaHenleggelse(Behandling sisteYtelseBehandling, BehandlingÅrsakType revurderingsÅrsak) {
        // Ifm køhåndtering - kun relevant for Foreldrepenger. REGSØK har relevant logikk for FØRSTEGANG.
        // Må håndtere revurderinger med åpent aksjonspunkt: Kopier med siste papirsøknad hvis finnes så AP reutledes i REGSØK
        boolean uregistrertPapirSøknadFP = sisteYtelseBehandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER);
        henleggBehandling(sisteYtelseBehandling);
        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(sisteYtelseBehandling.getType())) {
            return opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(sisteYtelseBehandling.getFagsak(), revurderingsÅrsak, sisteYtelseBehandling,false);
        }
        Behandling revurdering = opprettRevurdering(sisteYtelseBehandling.getFagsak(), revurderingsÅrsak);

        if (uregistrertPapirSøknadFP) {
            kopierPapirsøknadVedBehov(sisteYtelseBehandling, revurdering);
        }
        opprettInntektsmeldingerFraMottatteDokumentPåNyBehandling(revurdering);
        kopierVedlegg(sisteYtelseBehandling, revurdering);

        // Kopier behandlingsårsaker fra forrige behandling
        new BehandlingÅrsak.Builder(sisteYtelseBehandling.getBehandlingÅrsaker().stream()
            .map(BehandlingÅrsak::getBehandlingÅrsakType)
            .collect(toList()))
            .buildFor(revurdering);

        BehandlingskontrollKontekst nyKontekst = behandlingskontrollTjeneste.initBehandlingskontroll(revurdering);
        behandlingRepository.lagre(revurdering, nyKontekst.getSkriveLås());

        return revurdering;
    }

    public void henleggBehandling(Behandling behandling) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling.getId());
        behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(behandling, kontekst);
        behandlingskontrollTjeneste.henleggBehandling(kontekst, BehandlingResultatType.MERGET_OG_HENLAGT);
    }

    public void opprettInntektsmeldingerFraMottatteDokumentPåNyBehandling(Behandling nyBehandling) {
            hentAlleInntektsmeldingdokumenter(nyBehandling.getFagsakId()).stream()
                .sorted(MottattDokumentSorterer.sorterMottattDokument())
                .forEach(mottattDokument ->
                    dokumentPersistererTjeneste.persisterDokumentinnhold(mottattDokument, nyBehandling));

    }

    private void kopierPapirsøknadVedBehov(Behandling opprinneligBehandling, Behandling nyBehandling) {
        Optional<MottattDokument> søknad = mottatteDokumentTjeneste.hentMottatteDokumentFagsak(opprinneligBehandling.getFagsakId()).stream()
            .filter(MottattDokument::erSøknadsDokument)
            .max(Comparator.comparing(MottattDokument::getOpprettetTidspunkt))
            .filter(MottattDokument::erUstrukturertDokument);

        søknad.ifPresent(s -> {
            MottattDokument dokument = new MottattDokument.Builder(s)
                .medBehandlingId(nyBehandling.getId())
                .build();
            mottatteDokumentRepository.lagre(dokument);
        });
    }

    private void kopierVedlegg(Behandling opprinneligBehandling, Behandling nyBehandling) {
        List<MottattDokument> vedlegg = mottatteDokumentTjeneste.hentMottatteDokumentVedlegg(opprinneligBehandling.getId());

        if (!vedlegg.isEmpty()) {
            vedlegg.forEach(vedlegget -> {
                MottattDokument dokument = new MottattDokument.Builder(vedlegget)
                    .medBehandlingId(nyBehandling.getId())
                    .build();
                mottatteDokumentRepository.lagre(dokument);
            });
        }
    }

    private List<MottattDokument> hentAlleInntektsmeldingdokumenter(Long fagsakId) {
        return mottatteDokumentTjeneste.hentMottatteDokumentFagsak(fagsakId).stream()
            .filter(dok -> DokumentTypeId.INNTEKTSMELDING.equals(dok.getDokumentType()))
            .collect(toList());
    }


    public void settSomKøet(Behandling nyKøetBehandling) {
        behandlingskontrollTjeneste.settBehandlingPåVent(nyKøetBehandling, AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING, null, null, Venteårsak.VENT_ÅPEN_BEHANDLING);
    }

    public boolean harBehandlingsresultatOpphørt(Behandling behandling) {
        Optional<Behandlingsresultat> behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
        return behandlingsresultat.map(Behandlingsresultat::isBehandlingsresultatOpphørt).orElse(false);
    }

    public boolean erAvslåttBehandling(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).map(Behandlingsresultat::isBehandlingsresultatAvslått).orElse(false);
    }

    private void kopierTidligereGrunnlagFraTil(Fagsak fagsak, Behandling behandlingMedSøknad, Behandling nyBehandling) {
        SøknadEntitet søknad = søknadRepository.hentSøknad(behandlingMedSøknad);
        if (søknad != null) {
            søknadRepository.lagreOgFlush(nyBehandling, søknad);
        }
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        revurderingTjeneste.kopierAlleGrunnlagFraTidligereBehandling(behandlingMedSøknad, nyBehandling);
    }

    public Behandling opprettNyFørstegangsbehandlingFraTidligereSøknad(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Behandling behandlingMedSøknad) {
        Optional<Behandling> sisteYtelsesbehandling = revurderingRepository.hentSisteYtelsesbehandling(fagsak.getId());
        boolean harÅpenBehandling = !sisteYtelsesbehandling.map(Behandling::erSaksbehandlingAvsluttet).orElse(Boolean.TRUE);
        Behandling behandling = harÅpenBehandling ? oppdaterBehandlingViaHenleggelse(sisteYtelsesbehandling.get(), behandlingÅrsakType)
            : opprettFørstegangsbehandling(fagsak, behandlingÅrsakType, Optional.of(behandlingMedSøknad));

        kopierTidligereGrunnlagFraTil(fagsak, behandlingMedSøknad, behandling);
        return behandling;
    }

    public boolean erBehandlingOgFørstegangsbehandlingHenlagt(Fagsak fagsak) {
        Optional<Behandling> behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        Optional<Behandlingsresultat> behandlingsresultat = behandling.flatMap(b -> behandlingsresultatRepository.hentHvisEksisterer(b.getId()));
        if (behandlingsresultat.map(Behandlingsresultat::isBehandlingsresultatHenlagt).orElse(false)) {
            Optional<Behandling> førstegangsbehandling = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(fagsak.getId(), BehandlingType.FØRSTEGANGSSØKNAD);
            Optional<Behandlingsresultat> førstegangsbehandlingBehandlingsresultat = førstegangsbehandling.flatMap(b -> behandlingsresultatRepository.hentHvisEksisterer(b.getId()));
            return førstegangsbehandlingBehandlingsresultat.map(Behandlingsresultat::isBehandlingsresultatHenlagt).orElse(false);
        }
        return false;
    }
}
