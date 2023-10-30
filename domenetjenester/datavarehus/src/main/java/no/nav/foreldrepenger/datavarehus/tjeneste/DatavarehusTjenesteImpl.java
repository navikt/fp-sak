package no.nav.foreldrepenger.datavarehus.tjeneste;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
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
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.totrinn.TotrinnRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.datavarehus.domene.DatavarehusRepository;
import no.nav.foreldrepenger.datavarehus.xml.DvhVedtakXmlTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class DatavarehusTjenesteImpl implements DatavarehusTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(DatavarehusTjenesteImpl.class);

    private DatavarehusRepository datavarehusRepository;
    private FagsakRepository fagsakRepository;
    private FagsakEgenskapRepository fagsakEgenskapRepository;
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
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private SvangerskapspengerRepository svangerskapspengerRepository;

    @Inject
    public DatavarehusTjenesteImpl(BehandlingRepositoryProvider repositoryProvider, // NOSONAR
                                   DatavarehusRepository datavarehusRepository,
                                   BehandlingsresultatRepository behandlingsresultatRepository,
                                   TotrinnRepository totrinnRepository,
                                   FagsakEgenskapRepository fagsakEgenskapRepository,
                                   AnkeRepository ankeRepository,
                                   KlageRepository klageRepository,
                                   MottatteDokumentRepository mottatteDokumentRepository,
                                   DvhVedtakXmlTjeneste dvhVedtakXmlTjeneste,
                                   SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                   SvangerskapspengerRepository svangerskapspengerRepository) {
        this.datavarehusRepository = datavarehusRepository;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.totrinnRepository = totrinnRepository;
        this.klageRepository = klageRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.ankeRepository = ankeRepository;
        this.dvhVedtakXmlTjeneste = dvhVedtakXmlTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
        this.svangerskapspengerRepository = svangerskapspengerRepository;
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
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
        var totrinnsvurderings = totrinnRepository.hentTotrinnaksjonspunktvurderinger(behandling.getId());
        for (var aksjonspunkt : aksjonspunkter) {
            if (aksjonspunkt.getId() != null) {
                var godkjennt = totrinnsvurderings.stream().anyMatch(ttv -> ttv.getAksjonspunktDefinisjon() == aksjonspunkt.getAksjonspunktDefinisjon() && ttv.isGodkjent());
                var aksjonspunktDvh = AksjonspunktDvhMapper.map(aksjonspunkt, behandling, behandlingStegTilstand, godkjennt);
                datavarehusRepository.lagre(aksjonspunktDvh);
            }
        }
    }

    @Override
    public void lagreNedBehandling(Long behandlingId) {
        lagreNedBehandling(behandlingRepository.hentBehandling(behandlingId));
    }

    public void lagreNedBehandlingHistorisk(Behandling behandling, LocalDateTime funksjonellTid) {
        var vedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId());
        lagreNedBehandling(behandling, vedtak, funksjonellTid);
    }

    private void lagreNedBehandling(Behandling behandling) {
        var vedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId());
        lagreNedBehandling(behandling, vedtak, LocalDateTime.now());
    }

    private void lagreNedBehandling(Behandling behandling, Optional<BehandlingVedtak> vedtak, LocalDateTime funksjonellTid) {
        var familieHendelseGrunnlag = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandling.getId());
        var gjeldendeKlagevurderingresultat = klageRepository.hentKlageResultatHvisEksisterer(behandling.getId());
        var gjeldendeAnkevurderingresultat = ankeRepository.hentAnkeResultat(behandling.getId());
        var skjæringstidspunkt = familieHendelseGrunnlag.map(fhg -> skjæringstidspunkt(behandling, familieHendelseGrunnlag.get()));
        var mottatteDokumenter = mottatteDokumentRepository.hentMottatteDokument(behandling.getId()).stream()
            .filter(md -> md.getJournalpostId() != null && erRelevantDoument(behandling, md))
            .toList();
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
        var utlandMarkering = fagsakEgenskapRepository.finnFagsakMarkering(behandling.getFagsakId()).orElse(FagsakMarkering.NASJONAL);
        var forventetOppstart = forventetOppstartDato(behandling, skjæringstidspunkt.orElse(null));
        var behandlingDvh = BehandlingDvhMapper.map(behandling, behandlingsresultat.orElse(null),
            mottatteDokumenter, vedtak, familieHendelseGrunnlag, gjeldendeKlagevurderingresultat, gjeldendeAnkevurderingresultat,
            skjæringstidspunkt.flatMap(Skjæringstidspunkt::getSkjæringstidspunktHvisUtledet), utlandMarkering, forventetOppstart, funksjonellTid);
        datavarehusRepository.lagre(behandlingDvh);
    }

    private Skjæringstidspunkt skjæringstidspunkt(Behandling behandling,
                                                   FamilieHendelseGrunnlagEntitet fh) {
        if (!FamilieHendelseType.UDEFINERT.equals(fh.getGjeldendeVersjon().getType())) {
            try {
                return skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
            } catch (Exception e) {
                LOG.warn("Kunne ikke utlede skjæringstidspunkter for behandling {} antagelig henlagt ufullstendig behandling",
                    behandling.getId());
            }
        }
        return null;
    }

    // Samme logikk som FP-los for å være konsekvent
    // Førstegang: Bruk førsteUttak eller STP. Revurdering: Endringsdato, første dato fra endringssøknad, eller førsteUttak/STP
    private Optional<LocalDate> forventetOppstartDato(Behandling behandling, Skjæringstidspunkt skjæringstidspunkt) {
        if (FagsakYtelseType.UDEFINERT.equals(behandling.getFagsakYtelseType()) || !behandling.erYtelseBehandling()) {
            return Optional.empty();
        }
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType()) || BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            return finnUttakEllerUtledetSkjæringstidspunkt(behandling, skjæringstidspunkt);
        }
        var endretUttakFom = FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType()) ?
            finnEndringsdatoForeldrepenger(behandling) : finnEndringsdatoSvangerskapspenger(behandling);
        return endretUttakFom.or(() -> finnUttakEllerUtledetSkjæringstidspunkt(behandling, skjæringstidspunkt));
    }

    private Optional<LocalDate> finnUttakEllerUtledetSkjæringstidspunkt(Behandling behandling, Skjæringstidspunkt skjæringstidspunkt) {
        try {
            return Optional.ofNullable(skjæringstidspunkt).flatMap(Skjæringstidspunkt::getFørsteUttaksdatoSøknad)
                .or(() -> Optional.ofNullable(skjæringstidspunkt).flatMap(Skjæringstidspunkt::getSkjæringstidspunktHvisUtledet));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<LocalDate> finnEndringsdatoForeldrepenger(Behandling behandling) {
        var aggregat = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandling.getId());
        var endringsdato = aggregat.flatMap(YtelseFordelingAggregat::getAvklarteDatoer).map(AvklarteUttakDatoerEntitet::getGjeldendeEndringsdato);
        // Andre revurderinger enn endringssøknad har kopiert fordeling fra forrige behandling - kan ikke se på dem.
        return !behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER) ? endringsdato :
            endringsdato.or(() -> aggregat.map(YtelseFordelingAggregat::getGjeldendeFordeling)
                .map(OppgittFordelingEntitet::getPerioder).orElse(List.of()).stream()
                .map(OppgittPeriodeEntitet::getFom)
                .min(Comparator.naturalOrder()));
    }

    private Optional<LocalDate> finnEndringsdatoSvangerskapspenger(Behandling behandling) {
        if (!behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)) {
            return Optional.empty();
        }
        return svangerskapspengerRepository.hentGrunnlag(behandling.getId()).map(SvpGrunnlagEntitet::getGjeldendeVersjon)
            .map(SvpTilretteleggingerEntitet::getTilretteleggingListe).orElse(List.of()).stream()
            .filter(te -> !te.getKopiertFraTidligereBehandling() && te.getSkalBrukes())
            .map(SvpTilretteleggingEntitet::getTilretteleggingFOMListe)
            .flatMap(Collection::stream)
            .map(TilretteleggingFOM::getFomDato)
            .min(Comparator.naturalOrder());
    }

    private boolean erRelevantDoument(Behandling behandling, MottattDokument mottattDokument) {
        return switch (behandling.getType()) {
            case FØRSTEGANGSSØKNAD, REVURDERING -> mottattDokument.getDokumentType().erSøknadType() || mottattDokument.getDokumentType().erEndringsSøknadType();
            case KLAGE, ANKE -> DokumentTypeId.KLAGE_DOKUMENT.equals(mottattDokument.getDokumentType());
            default -> false;
        };
    }

    @Override
    public void lagreNedVedtak(BehandlingVedtak vedtak, Behandling behandling) {
        var ytelseMedUtbetalingFra = finnUtbetaltDato(behandling, vedtak);

        var behandlingVedtakDvh = BehandlingVedtakDvhMapper.map(vedtak, behandling, ytelseMedUtbetalingFra);
        datavarehusRepository.lagre(behandlingVedtakDvh);

        lagreNedBehandling(behandling, Optional.of(vedtak), LocalDateTime.now());
    }

    private LocalDate finnUtbetaltDato(Behandling behandling, BehandlingVedtak vedtak) {
        if (!behandling.erYtelseBehandling()) {
            return null;
        }
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            return VedtakResultatType.INNVILGET.equals(vedtak.getVedtakResultatType()) ? LocalDate.now() : null;
        } else {
            // TODO tilby min-dato i BR-entitet og erstatt slike tilfelle som dette
            return beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.getId())
                .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
                .filter(p -> p.getDagsats() > 0)
                .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
                .min(Comparator.naturalOrder())
                .orElse(null);
        }
    }

    public void lagreNedVedtakInnsyn(BehandlingVedtak vedtak, Behandling behandling) {
        var behandlingVedtakDvh = BehandlingVedtakDvhMapper.mapInnsynRepop(vedtak, behandling);
        datavarehusRepository.lagre(behandlingVedtakDvh);
    }

    @Override
    public void opprettOgLagreVedtakXml(Long behandlingId) {
        var behandlingVedtakOpt = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandlingVedtakOpt.isPresent()) {
            var vedtakXml = dvhVedtakXmlTjeneste.opprettDvhVedtakXml(behandlingId);
            var hendelseType = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandling.getId())
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
        return AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP.equals(a.getAksjonspunktDefinisjon());
    }

    private boolean gjelderKlageVurderingResultat(Aksjonspunkt a) {
        return AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP.equals(a.getAksjonspunktDefinisjon())
            || AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_KLAGE.equals(a.getAksjonspunktDefinisjon());
    }

    private boolean gjelderAnkeVurderingResultat(Aksjonspunkt a) {
        return AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_ANKE.equals(a.getAksjonspunktDefinisjon()) ||
            AksjonspunktDefinisjon.AUTO_VENT_ANKE_OVERSENDT_TIL_TRYGDERETTEN.equals(a.getAksjonspunktDefinisjon());
    }

}
