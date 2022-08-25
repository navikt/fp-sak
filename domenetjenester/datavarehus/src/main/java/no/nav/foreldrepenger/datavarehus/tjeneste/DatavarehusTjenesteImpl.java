package no.nav.foreldrepenger.datavarehus.tjeneste;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegTilstandSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
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
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.datavarehus.domene.DatavarehusRepository;
import no.nav.foreldrepenger.datavarehus.xml.DvhVedtakXmlTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnRepository;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class DatavarehusTjenesteImpl implements DatavarehusTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(DatavarehusTjenesteImpl.class);

    private DatavarehusRepository datavarehusRepository;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private PersonopplysningRepository personopplysningRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private FamilieHendelseRepository familieGrunnlagRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private TotrinnRepository totrinnRepository;
    private KlageRepository klageRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private AnkeRepository ankeRepository;
    private DvhVedtakXmlTjeneste dvhVedtakXmlTjeneste;
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @Inject
    public DatavarehusTjenesteImpl(BehandlingRepositoryProvider repositoryProvider,
                                   DatavarehusRepository datavarehusRepository,
                                   BehandlingsresultatRepository behandlingsresultatRepository,
                                   TotrinnRepository totrinnRepository,
                                   AnkeRepository ankeRepository,
                                   KlageRepository klageRepository,
                                   MottatteDokumentRepository mottatteDokumentRepository,
                                   DvhVedtakXmlTjeneste dvhVedtakXmlTjeneste,
                                   ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste,
                                   SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.datavarehusRepository = datavarehusRepository;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.totrinnRepository = totrinnRepository;
        this.klageRepository = klageRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.ankeRepository = ankeRepository;
        this.dvhVedtakXmlTjeneste = dvhVedtakXmlTjeneste;
        this.foreldrepengerUttakTjeneste = foreldrepengerUttakTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    public DatavarehusTjenesteImpl() {
        //Crazy Dedicated Instructors
    }

    @Override
    public void lagreNedFagsakRelasjon(FagsakRelasjon fr) {

        var fagsakRelasjonDvh = FagsakRelasjonDvhMapper.map(fr);
        datavarehusRepository.lagre(fagsakRelasjonDvh);
    }

    @Override
    public void lagreNedFagsak(Long fagsakId) {
        var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId);
        Optional<AktørId> annenPartAktørId = Optional.empty();
        if (behandling.isPresent()) {
            annenPartAktørId = personopplysningRepository.hentOppgittAnnenPartHvisEksisterer(behandling.get().getId()).map(OppgittAnnenPartEntitet::getAktørId);
        }
        var fagsakDvh = FagsakDvhMapper.map(fagsak, annenPartAktørId);
        datavarehusRepository.lagre(fagsakDvh);
    }

    @Override
    public void lagreNedAksjonspunkter(Collection<Aksjonspunkt> aksjonspunkter, Long behandlingId, BehandlingStegType behandlingStegType) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var behandlingStegTilstand = behandling.getBehandlingStegTilstand(behandlingStegType);
        var totrinnsvurderings = totrinnRepository.hentTotrinnaksjonspunktvurderinger(behandling);
        for (var aksjonspunkt : aksjonspunkter) {
            if (aksjonspunkt.getId() != null) {
                var godkjennt = totrinnsvurderings.stream().anyMatch(ttv -> ttv.getAksjonspunktDefinisjon() == aksjonspunkt.getAksjonspunktDefinisjon() && ttv.isGodkjent());
                var aksjonspunktDvh = AksjonspunktDvhMapper.map(aksjonspunkt, behandling, behandlingStegTilstand, godkjennt);
                datavarehusRepository.lagre(aksjonspunktDvh);
            }
        }
    }

    @Override
    public void lagreNedBehandlingStegTilstand(Long behandlingId, BehandlingStegTilstandSnapshot tilTilstand) {
        var behandlingStegDvh = BehandlingStegDvhMapper.map(tilTilstand, behandlingId);
        datavarehusRepository.lagre(behandlingStegDvh);
    }

    @Override
    public void lagreNedBehandling(Long behandlingId) {
        lagreNedBehandling(behandlingRepository.hentBehandling(behandlingId));
    }

    private void lagreNedBehandling(Behandling behandling) {
        var vedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId());
        lagreNedBehandling(behandling, vedtak);
    }

    private void lagreNedBehandling(Behandling behandling, Optional<BehandlingVedtak> vedtak) {
        var fh = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandling.getId());
        var gjeldendeKlagevurderingresultat = klageRepository.hentKlageResultatHvisEksisterer(behandling.getId());
        var gjeldendeAnkevurderingresultat = ankeRepository.hentAnkeResultat(behandling.getId());
        var uttak = foreldrepengerUttakTjeneste.hentUttakHvisEksisterer(behandling.getId());
        Optional<LocalDate> skjæringstidspunkt = uttak.isPresent() && fh.isPresent() ?
            skjæringstidspunkt(behandling, fh.get()) : Optional.empty();
        var mottattTidspunkt = finnMottattTidspunkt(behandling);
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
        var behandlingDvh = BehandlingDvhMapper.map(behandling, behandlingsresultat.orElse(null),
            mottattTidspunkt, vedtak, fh, gjeldendeKlagevurderingresultat, gjeldendeAnkevurderingresultat, uttak, skjæringstidspunkt);
        datavarehusRepository.lagre(behandlingDvh);
    }

    private Optional<LocalDate> skjæringstidspunkt(Behandling behandling,
                                                   FamilieHendelseGrunnlagEntitet fh) {
        if (!FamilieHendelseType.UDEFINERT.equals(fh.getGjeldendeVersjon().getType())) {
            try {
                return skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).getSkjæringstidspunktHvisUtledet();
            } catch (Exception e) {
                LOG.warn("Kunne ikke utlede skjæringstidspunkter for behandling {} antagelig henlagt ufullstendig behandling",
                    behandling.getId());
            }
        }
        return Optional.empty();
    }

    private LocalDateTime finnMottattTidspunkt(Behandling behandling) {
        var søknadOgKlageTyper = Stream.concat(DokumentTypeId.getSøknadTyper().stream(), Stream.of(DokumentTypeId.KLAGE_DOKUMENT)).collect(Collectors.toSet());
        var mottatteDokumenter = mottatteDokumentRepository.hentMottatteDokument(behandling.getId());

        return mottatteDokumenter.stream()
            .filter(o -> søknadOgKlageTyper.contains(o.getDokumentType())).findFirst() //Hent ut søknad eller klage mottattdato
            .or(() -> mottatteDokumenter.stream() //Eksisterer ikke søknad eller klage, hent ut mottattdato til første dokument knyttet til behandlingen.
                .min(Comparator.comparing(MottattDokument::getMottattDato)))
            .map(MottattDokument::getMottattTidspunkt).orElse(null);
    }

    @Override
    public void lagreNedVedtak(BehandlingVedtak vedtak, Behandling behandling) {
        var behandlingVedtakDvh = BehandlingVedtakDvhMapper.map(vedtak, behandling);
        datavarehusRepository.lagre(behandlingVedtakDvh);

        lagreNedBehandling(behandling, Optional.of(vedtak));
    }

    @Override
    public void opprettOgLagreVedtakXml(Long behandlingId) {
        var behandlingVedtakOpt = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandlingVedtakOpt.isPresent()) {
            var vedtakXml = dvhVedtakXmlTjeneste.opprettDvhVedtakXml(behandlingId);
            final var hendelseType = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandling.getId())
                .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
                .map(FamilieHendelseEntitet::getType)
                .orElse(FamilieHendelseType.UDEFINERT);
            var vedtakUtbetalingDvh = VedtakUtbetalingDvhMapper.map(vedtakXml, behandling, behandlingVedtakOpt.get(), hendelseType);
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
        var behandlingVedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        if (behandlingVedtak.isPresent()) {
            var eksisterende = datavarehusRepository.finn(behandlingId, behandlingVedtak.get().getId());
            if (eksisterende.isPresent()) {
                var vedtakXml = dvhVedtakXmlTjeneste.opprettDvhVedtakXml(behandlingId);
                datavarehusRepository.oppdater(behandling.getId(), behandlingVedtak.get().getId(), vedtakXml);
            } else {
                opprettOgLagreVedtakXml(behandlingId);
            }
        } else {
            throw new IllegalStateException(String.format("Finner ikke behandlingsvedtak på behandling %s vi skal oppdatere", behandlingId));
        }
    }

    private void lagreKlageFormkrav(KlageFormkravEntitet klageFormkrav) {
        var klageFormkravDvh = KlageFormkravDvhMapper.map(klageFormkrav);
        datavarehusRepository.lagre(klageFormkravDvh);

    }

    private void lagreKlageVurderingResultat(KlageVurderingResultat klageVurderingResultat) {
        var klageVurderingResultatDvh = KlageVurderingResultatDvhMapper.map(klageVurderingResultat);
        datavarehusRepository.lagre(klageVurderingResultatDvh);
    }

    private void lagreAnkeVurderingResultat(AnkeVurderingResultatEntitet ankeVurderingResultat) {
        var ankeVurderingResultatDvh = AnkeVurderingResultatDvhMapper.map(ankeVurderingResultat);
        datavarehusRepository.lagre(ankeVurderingResultatDvh);
    }

    @Override
    public void oppdaterHvisKlageEllerAnke(Long behandlingId, Collection<Aksjonspunkt> aksjonspunkter) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            aksjonspunkter.stream().filter(Aksjonspunkt::erUtført)
                .filter(a -> gjelderKlageFormkrav(a) || gjelderKlageVurderingResultat(a))
                .forEach(ap -> oppdaterMedKlage(behandling, ap));
        } else if (BehandlingType.ANKE.equals(behandling.getType())) {
            aksjonspunkter.stream().filter(Aksjonspunkt::erUtført)
                .filter(this::gjelderAnkeVurderingResultat)
                .forEach(ap -> oppdaterMedAnke(behandling));
        }
    }

    private void oppdaterMedKlage(Behandling behandling, Aksjonspunkt aksjonspunkt) {
        if (BehandlingType.KLAGE.equals(behandling.getType()) && gjelderKlageFormkrav(aksjonspunkt)) {
            klageRepository.hentGjeldendeKlageFormkrav(behandling.getId()).ifPresent(this::lagreKlageFormkrav);
        } else if (BehandlingType.KLAGE.equals(behandling.getType()) && gjelderKlageVurderingResultat(aksjonspunkt)) {
            klageRepository.hentGjeldendeKlageVurderingResultat(behandling).ifPresent(this::lagreKlageVurderingResultat);
        }
     }

     private void oppdaterMedAnke(Behandling behandling) {
        if (BehandlingType.ANKE.equals(behandling.getType())) {
            ankeRepository.hentAnkeVurderingResultat(behandling.getId()).ifPresent(this::lagreAnkeVurderingResultat);
        }
     }

    private boolean gjelderKlageFormkrav(Aksjonspunkt a) {
        return AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP.equals(a.getAksjonspunktDefinisjon())
            || AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_KA.equals(a.getAksjonspunktDefinisjon());
    }

    private boolean gjelderKlageVurderingResultat(Aksjonspunkt a) {
        return AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP.equals(a.getAksjonspunktDefinisjon())
            || AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_KLAGE.equals(a.getAksjonspunktDefinisjon());
    }

    private boolean gjelderAnkeVurderingResultat(Aksjonspunkt a) {
        return AksjonspunktDefinisjon.MANUELL_VURDERING_AV_ANKE.equals(a.getAksjonspunktDefinisjon())
            || AksjonspunktDefinisjon.MANUELL_VURDERING_AV_ANKE_MERKNADER.equals(a.getAksjonspunktDefinisjon())
            || AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_ANKE.equals(a.getAksjonspunktDefinisjon());
    }

}
