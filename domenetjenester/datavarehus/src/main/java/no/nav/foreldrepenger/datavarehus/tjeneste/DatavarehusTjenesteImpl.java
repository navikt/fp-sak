package no.nav.foreldrepenger.datavarehus.tjeneste;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegTilstandSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegTilstand;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.datavarehus.domene.AksjonspunktDvh;
import no.nav.foreldrepenger.datavarehus.domene.AnkeVurderingResultatDvh;
import no.nav.foreldrepenger.datavarehus.domene.BehandlingDvh;
import no.nav.foreldrepenger.datavarehus.domene.BehandlingStegDvh;
import no.nav.foreldrepenger.datavarehus.domene.BehandlingVedtakDvh;
import no.nav.foreldrepenger.datavarehus.domene.DatavarehusRepository;
import no.nav.foreldrepenger.datavarehus.domene.FagsakDvh;
import no.nav.foreldrepenger.datavarehus.domene.FagsakRelasjonDvh;
import no.nav.foreldrepenger.datavarehus.domene.KlageFormkravDvh;
import no.nav.foreldrepenger.datavarehus.domene.KlageVurderingResultatDvh;
import no.nav.foreldrepenger.datavarehus.domene.VedtakUtbetalingDvh;
import no.nav.foreldrepenger.datavarehus.xml.DvhVedtakXmlTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnRepository;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class DatavarehusTjenesteImpl implements DatavarehusTjeneste {

    private DatavarehusRepository datavarehusRepository;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private PersonopplysningRepository personopplysningRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private FamilieHendelseRepository familieGrunnlagRepository;
    private TotrinnRepository totrinnRepository;
    private KlageRepository klageRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private AnkeRepository ankeRepository;
    private DvhVedtakXmlTjeneste dvhVedtakXmlTjeneste;
    private UttakRepository uttakRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    public DatavarehusTjenesteImpl() {
        //Crazy Dedicated Instructors
    }

    @Inject
    public DatavarehusTjenesteImpl(BehandlingRepositoryProvider repositoryProvider,
                                   DatavarehusRepository datavarehusRepository,
                                   TotrinnRepository totrinnRepository,
                                   AnkeRepository ankeRepository,
                                   KlageRepository klageRepository,
                                   FamilieHendelseRepository familieHendelseRepository,
                                   PersonopplysningRepository personopplysningRepository,
                                   MottatteDokumentRepository mottatteDokumentRepository,
                                   BehandlingVedtakRepository behandlingVedtakRepository,
                                   DvhVedtakXmlTjeneste dvhVedtakXmlTjeneste,
                                   UttakRepository uttakRepository,
                                   SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.datavarehusRepository = datavarehusRepository;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.personopplysningRepository = personopplysningRepository;
        this.familieGrunnlagRepository = familieHendelseRepository;
        this.totrinnRepository = totrinnRepository;
        this.klageRepository = klageRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.ankeRepository = ankeRepository;
        this.dvhVedtakXmlTjeneste = dvhVedtakXmlTjeneste;
        this.uttakRepository = uttakRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public void lagreNedFagsakRelasjon(FagsakRelasjon fr) {

        FagsakRelasjonDvh fagsakRelasjonDvh = new FagsakRelasjonDvhMapper().map(fr);
        datavarehusRepository.lagre(fagsakRelasjonDvh);
    }

    @Override
    public void lagreNedFagsak(Long fagsakId) {
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        Optional<Behandling> behandling = behandlingRepository.hentSisteBehandlingForFagsakId(fagsakId);
        Optional<AktørId> annenPartAktørId = Optional.empty();
        if (behandling.isPresent()) {
            annenPartAktørId = personopplysningRepository.hentPersonopplysningerHvisEksisterer(behandling.get().getId())
                .flatMap(PersonopplysningGrunnlagEntitet::getOppgittAnnenPart).map(OppgittAnnenPartEntitet::getAktørId);
        }
        FagsakDvh fagsakDvh = new FagsakDvhMapper().map(fagsak, annenPartAktørId);
        datavarehusRepository.lagre(fagsakDvh);
    }

    @Override
    public void lagreNedAksjonspunkter(Collection<Aksjonspunkt> aksjonspunkter, Long behandlingId, BehandlingStegType behandlingStegType) {
        AksjonspunktDvhMapper mapper = new AksjonspunktDvhMapper();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Optional<BehandlingStegTilstand> behandlingStegTilstand = behandling.getBehandlingStegTilstand(behandlingStegType);
        Collection<Totrinnsvurdering> totrinnsvurderings = totrinnRepository.hentTotrinnaksjonspunktvurderinger(behandling);
        for (Aksjonspunkt aksjonspunkt : aksjonspunkter) {
            if (aksjonspunkt.getId() != null) {
                boolean godkjennt = totrinnsvurderings.stream().anyMatch(ttv -> ttv.getAksjonspunktDefinisjon() == aksjonspunkt.getAksjonspunktDefinisjon() && ttv.isGodkjent());
                AksjonspunktDvh aksjonspunktDvh = mapper.map(aksjonspunkt, behandling, behandlingStegTilstand, godkjennt);
                datavarehusRepository.lagre(aksjonspunktDvh);
            }
        }
    }

    @Override
    public void lagreNedBehandlingStegTilstand(Long behandlingId, BehandlingStegTilstandSnapshot tilTilstand) {
        BehandlingStegDvh behandlingStegDvh = new BehandlingStegDvhMapper().map(tilTilstand, behandlingId);
        datavarehusRepository.lagre(behandlingStegDvh);
    }

    @Override
    public void lagreNedBehandling(Long behandlingId) {
        lagreNedBehandling(behandlingRepository.hentBehandling(behandlingId));
    }

    @Override
    public void lagreNedBehandling(Behandling behandling) {
        Optional<BehandlingVedtak> vedtak = behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(behandling.getId());
        lagreNedBehandling(behandling, vedtak);
    }

    private void lagreNedBehandling(Behandling behandling, Optional<BehandlingVedtak> vedtak) {
        Optional<FamilieHendelseGrunnlagEntitet> fh = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandling.getId());
        Optional<KlageVurderingResultat> gjeldendeKlagevurderingresultat = klageRepository.hentGjeldendeKlageVurderingResultat(behandling);
        Optional<UttakResultatEntitet> ur = uttakRepository.hentUttakResultatHvisEksisterer(behandling.getId());
        Optional<LocalDate> skjæringstidspunkt  = Optional.empty();
        if(!ur.isPresent() && !FamilieHendelseType.UDEFINERT.equals(fh.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon).map(FamilieHendelseEntitet::getType).orElse(FamilieHendelseType.UDEFINERT))){
            skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).getSkjæringstidspunktHvisUtledet();
        }
        LocalDateTime mottattTidspunkt = finnMottattTidspunkt(behandling);
        BehandlingDvh behandlingDvh = new BehandlingDvhMapper().map(behandling, mottattTidspunkt, vedtak, fh, gjeldendeKlagevurderingresultat,ur,skjæringstidspunkt);
        datavarehusRepository.lagre(behandlingDvh);
    }

    private LocalDateTime finnMottattTidspunkt(Behandling behandling) {
        Set<String> søknadOgKlageTyper = Stream.concat(DokumentTypeId.getSøknadTyper().stream(), Stream.of(DokumentTypeId.KLAGE_DOKUMENT.getKode())).collect(Collectors.toSet());
        List<MottattDokument> mottatteDokumenter = mottatteDokumentRepository.hentMottatteDokument(behandling.getId());

        return mottatteDokumenter.stream()
            .filter(o -> søknadOgKlageTyper.contains(o.getDokumentType().getKode())).findFirst() //Hent ut søknad eller klage mottattdato
            .or(() -> mottatteDokumenter.stream() //Eksisterer ikke søknad eller klage, hent ut mottattdato til første dokument knyttet til behandlingen.
                .min(Comparator.comparing(MottattDokument::getMottattDato)))
            .map(MottattDokument::getMottattTidspunkt).orElse(null);
    }

    @Override
    public void lagreNedVedtak(BehandlingVedtak vedtak, Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        BehandlingVedtakDvh behandlingVedtakDvh = new BehandlingVedtakDvhMapper().map(vedtak, behandling);
        datavarehusRepository.lagre(behandlingVedtakDvh);

        lagreNedBehandling(behandling, Optional.of(vedtak));
    }

    @Override
    public void opprettOgLagreVedtakXml(Long behandlingId) {
        var behandlingVedtakOpt = behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandlingVedtakOpt.isPresent()) {
            String vedtakXml = dvhVedtakXmlTjeneste.opprettDvhVedtakXml(behandlingId);
            final FamilieHendelseType hendelseType = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandling.getId())
                .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
                .map(FamilieHendelseEntitet::getType)
                .orElse(FamilieHendelseType.UDEFINERT);
            VedtakUtbetalingDvh vedtakUtbetalingDvh = new VedtakUtbetalingDvhMapper().map(vedtakXml, behandling, behandlingVedtakOpt.get(), hendelseType);
            datavarehusRepository.lagre(vedtakUtbetalingDvh);
        }
    }

    @Override
    public List<Long> hentVedtakBehandlinger(LocalDateTime fom, LocalDateTime tom) {
        return datavarehusRepository.hentVedtakBehandlinger(fom, tom);
    }

    @Override
    public List<Long> hentVedtakBehandlinger(Long behandlingid) {
        return datavarehusRepository.hentVedtakBehandlinger(behandlingid);
    }

    @Override
    public void oppdaterVedtakXml(Long behandlingId) {
        Optional<BehandlingVedtak> behandlingVedtak = behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(behandlingId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        if (behandlingVedtak.isPresent()) {
            Optional<VedtakUtbetalingDvh> eksisterende = datavarehusRepository.finn(behandlingId, behandlingVedtak.get().getId());
            if (eksisterende.isPresent()) {
                String vedtakXml = dvhVedtakXmlTjeneste.opprettDvhVedtakXml(behandlingId);
                datavarehusRepository.oppdater(behandling.getId(), behandlingVedtak.get().getId(), vedtakXml);
            } else {
                opprettOgLagreVedtakXml(behandlingId);
            }
        } else {
            throw new IllegalStateException(String.format("Finner ikke behandlingsvedtak på behandling %s vi skal oppdatere", behandlingId));
        }
    }

    private void lagreKlageFormkrav(KlageFormkravEntitet klageFormkrav) {
        KlageFormkravDvh klageFormkravDvh = new KlageFormkravDvhMapper().map(klageFormkrav);
        datavarehusRepository.lagre(klageFormkravDvh);

    }

    private void lagreKlageVurderingResultat(KlageVurderingResultat klageVurderingResultat) {
        KlageVurderingResultatDvh klageVurderingResultatDvh = new KlageVurderingResultatDvhMapper().map(klageVurderingResultat);
        datavarehusRepository.lagre(klageVurderingResultatDvh);
    }

    private void lagreAnkeVurderingResultat(AnkeVurderingResultatEntitet ankeVurderingResultat) {
        AnkeVurderingResultatDvh ankeVurderingResultatDvh = new AnkeVurderingResultatDvhMapper().map(ankeVurderingResultat);
        datavarehusRepository.lagre(ankeVurderingResultatDvh);
    }
    @Override
    public void oppdaterHvisKlageEllerAnke(Long behandlingId, Collection<Aksjonspunkt> aksjonspunkter) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            aksjonspunkter.stream().filter(Aksjonspunkt::erUtført).filter(a -> a.gjelderKlageFormkrav() || a.gjelderKlageVurderingResultat()).forEach(ap -> oppdaterMedKlage(behandling, ap));
        } else if (BehandlingType.ANKE.equals(behandling.getType())) {
            aksjonspunkter.stream().filter(Aksjonspunkt::erUtført).filter(Aksjonspunkt::gjelderAnkeVurderingResultat).forEach(ap -> oppdaterMedAnke(behandling));
        }
    }

    private void oppdaterMedKlage(Behandling behandling, Aksjonspunkt aksjonspunkt) {
        if (BehandlingType.KLAGE.equals(behandling.getType()) && aksjonspunkt.gjelderKlageFormkrav()) {
            klageRepository.hentGjeldendeKlageFormkrav(behandling).ifPresent(this::lagreKlageFormkrav);
        } else if (BehandlingType.KLAGE.equals(behandling.getType()) && aksjonspunkt.gjelderKlageVurderingResultat()) {
            klageRepository.hentGjeldendeKlageVurderingResultat(behandling).ifPresent(this::lagreKlageVurderingResultat);
        }
     }

     private void oppdaterMedAnke(Behandling behandling) {
        if (BehandlingType.ANKE.equals(behandling.getType())) {
            ankeRepository.hentAnkeVurderingResultat(behandling.getId()).ifPresent(this::lagreAnkeVurderingResultat);
        }
     }

}
