package no.nav.foreldrepenger.kompletthet.implV2;

import static java.util.Collections.emptyList;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadVedleggEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;

@ApplicationScoped
public class ManglendeVedleggTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ManglendeVedleggTjeneste.class);

    private SøknadRepository søknadRepository;
    private DokumentArkivTjeneste dokumentArkivTjeneste;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlingRepository behandlingRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;

    public ManglendeVedleggTjeneste() {
        // CDI
    }

    @Inject
    public ManglendeVedleggTjeneste(SøknadRepository søknadRepository,
                                    DokumentArkivTjeneste dokumentArkivTjeneste,
                                    BehandlingVedtakRepository behandlingVedtakRepository,
                                    MottatteDokumentRepository mottatteDokumentRepository,
                                    BehandlingRepository behandlingRepository,
                                    YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.søknadRepository = søknadRepository;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.behandlingRepository = behandlingRepository;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    public List<ManglendeVedlegg> utledManglendeVedleggForSøknad(BehandlingReferanse ref) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(ref.fagsakYtelseType())) {
            var søknad = søknadRepository.hentSøknadHvisEksisterer(ref.behandlingId());

            // Manuelt registrerte søknader har foreløpig ikke vedleggsliste og kan derfor ikke kompletthetssjekkes
            if (søknad.isEmpty() || !søknad.get().getElektroniskRegistrert() || søknad.get().getSøknadVedlegg() == null
                    || søknad.get().getSøknadVedlegg().isEmpty()) {
                return emptyList();
            }
        }

        return ref.erRevurdering() ? utledManglendeVedleggForRevurdering(ref) : utledManglendeVedleggForFørstegangssøknad(ref);
    }

    private List<ManglendeVedlegg> utledManglendeVedleggForFørstegangssøknad(BehandlingReferanse ref) {
        var søknad = søknadRepository.hentSøknadHvisEksisterer(ref.behandlingId());
        var dokumentTypeIds = dokumentArkivTjeneste.hentDokumentTypeIdForSak(ref.saksnummer(), LocalDate.MIN);
        var manglendeVedlegg = identifiserManglendeVedlegg(søknad, dokumentTypeIds);

        if (!manglendeVedlegg.isEmpty()) {
            LOG.info("Behandling {} er ikke komplett - mangler følgende vedlegg til søknad: {}", ref.behandlingId(), lagDokumentTypeString(manglendeVedlegg));
        }

        return manglendeVedlegg;
    }

    private List<ManglendeVedlegg> utledManglendeVedleggForRevurdering(BehandlingReferanse ref) {
        var behandlingId = ref.behandlingId();
        var søknad = søknadRepository.hentSøknadHvisEksisterer(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var vedtaksdato = behandlingVedtakRepository.hentBehandlingVedtakFraRevurderingensOriginaleBehandling(behandling).getVedtaksdato();
        var sammenligningsdato = søknad.map(SøknadEntitet::getSøknadsdato).filter(vedtaksdato::isAfter).orElse(vedtaksdato);

        Set<DokumentTypeId> arkivDokumentTypeIds = new HashSet<>(dokumentArkivTjeneste.hentDokumentTypeIdForSak(ref.saksnummer(), sammenligningsdato));
        mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(ref.fagsakId()).stream()
                .filter(m -> !m.getMottattDato().isBefore(sammenligningsdato))
                .map(MottattDokument::getDokumentType)
                .forEach(arkivDokumentTypeIds::add);
        arkivDokumentTypeIds.addAll(DokumentTypeId.ekvivalenter(arkivDokumentTypeIds));

        var manglendeVedlegg = new ArrayList<>(identifiserManglendeVedlegg(søknad, arkivDokumentTypeIds));
        var oppgittFordeling = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId).map(YtelseFordelingAggregat::getOppgittFordeling);
        var manglendeVedleggUtsettelse = identifiserManglendeVedleggSomFølgerAvUtsettelse(oppgittFordeling, arkivDokumentTypeIds);
        manglendeVedlegg.addAll(manglendeVedleggUtsettelse);

        if (!manglendeVedlegg.isEmpty()) {
            LOG.info("Revurdering {} er ikke komplett - mangler følgende vedlegg til søknad: {}", behandlingId,
                    lagDokumentTypeString(manglendeVedlegg));
        }
        return manglendeVedlegg;
    }

    private List<ManglendeVedlegg> identifiserManglendeVedlegg(Optional<SøknadEntitet> søknad, Set<DokumentTypeId> dokumentTypeIds) {
        return getSøknadVedleggListe(søknad)
                .stream()
                .filter(SøknadVedleggEntitet::isErPåkrevdISøknadsdialog)
                .map(SøknadVedleggEntitet::getSkjemanummer)
                .map(this::finnDokumentTypeId)
                .filter(doc -> !dokumentTypeIds.contains(doc))
                .map(ManglendeVedlegg::new)
                .toList();
    }

    private Set<SøknadVedleggEntitet> getSøknadVedleggListe(Optional<SøknadEntitet> søknad) {
        if (søknad.map(SøknadEntitet::getElektroniskRegistrert).orElse(false)) {
            return søknad.map(SøknadEntitet::getSøknadVedlegg)
                    .orElse(Collections.emptySet());
        }
        return Collections.emptySet();
    }

    private DokumentTypeId finnDokumentTypeId(String dokumentTypeIdKode) {
        DokumentTypeId dokumentTypeId;
        try {
            dokumentTypeId = DokumentTypeId.finnForKodeverkEiersKode(dokumentTypeIdKode);
        } catch (NoResultException e) {
            // skal tåle dette
            dokumentTypeId = DokumentTypeId.UDEFINERT;
        }
        return dokumentTypeId;
    }

    private List<ManglendeVedlegg> identifiserManglendeVedleggSomFølgerAvUtsettelse(Optional<OppgittFordelingEntitet> oppgittFordeling,
                                                                                    Set<DokumentTypeId> dokumentTypeIdSet) {
        if (oppgittFordeling.isEmpty()) {
            return emptyList();
        }

        List<ManglendeVedlegg> manglendeVedlegg = new ArrayList<>();
        var oppgittePerioder = oppgittFordeling.get().getPerioder();

        oppgittePerioder.stream().map(OppgittPeriodeEntitet::getÅrsak).forEach(årsak -> {
            if (UtsettelseÅrsak.SYKDOM.equals(årsak) && !dokumentTypeIdSet.contains(DokumentTypeId.LEGEERKLÆRING)) {
                manglendeVedlegg.add(new ManglendeVedlegg(DokumentTypeId.LEGEERKLÆRING));
            } else if ((UtsettelseÅrsak.INSTITUSJON_SØKER.equals(årsak) || UtsettelseÅrsak.INSTITUSJON_BARN.equals(årsak))
                    && !dokumentTypeIdSet.contains(DokumentTypeId.DOK_INNLEGGELSE)) {
                manglendeVedlegg.add(new ManglendeVedlegg(DokumentTypeId.DOK_INNLEGGELSE));
            }  else if (UtsettelseÅrsak.HV_OVELSE.equals(årsak) && !dokumentTypeIdSet.contains(DokumentTypeId.DOK_HV)) {
                manglendeVedlegg.add(new ManglendeVedlegg(DokumentTypeId.DOK_HV));
            }   else if (UtsettelseÅrsak.NAV_TILTAK.equals(årsak) && !dokumentTypeIdSet.contains(DokumentTypeId.DOK_NAV_TILTAK)) {
                manglendeVedlegg.add(new ManglendeVedlegg(DokumentTypeId.DOK_NAV_TILTAK));
            }
        });

        return manglendeVedlegg;
    }

    private String lagDokumentTypeString(List<ManglendeVedlegg> manglendeVedlegg) {
        return manglendeVedlegg.stream().map(mv -> mv.getDokumentType().getKode()).toList().toString();
    }
}
