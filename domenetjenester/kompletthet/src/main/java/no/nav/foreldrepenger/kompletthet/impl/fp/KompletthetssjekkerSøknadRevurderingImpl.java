package no.nav.foreldrepenger.kompletthet.impl.fp;

import static java.util.Collections.emptyList;

import java.time.Period;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

@ApplicationScoped
@BehandlingTypeRef(BehandlingType.REVURDERING)
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
public class KompletthetssjekkerSøknadRevurderingImpl extends KompletthetssjekkerSøknadImpl {
    private static final Logger LOG = LoggerFactory.getLogger(KompletthetssjekkerSøknadRevurderingImpl.class);

    private SøknadRepository søknadRepository;
    private DokumentArkivTjeneste dokumentArkivTjeneste;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;

    private BehandlingRepository behandlingRepository;

    private YtelsesFordelingRepository ytelsesFordelingRepository;

    KompletthetssjekkerSøknadRevurderingImpl() {
    }

    @Inject
    public KompletthetssjekkerSøknadRevurderingImpl(DokumentArkivTjeneste dokumentArkivTjeneste,
                                                    BehandlingRepositoryProvider repositoryProvider,
                                                    @KonfigVerdi(value = "fp.ventefrist.tidlig.soeknad", defaultVerdi = "P4W") Period ventefristForTidligSøknad) {
        super(ventefristForTidligSøknad,
            repositoryProvider.getSøknadRepository(), repositoryProvider.getMottatteDokumentRepository());
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.mottatteDokumentRepository = repositoryProvider.getMottatteDokumentRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
    }

    /**
     * Spør Joark om dokumentliste og sjekker det som finnes i vedleggslisten på søknaden mot det som ligger i Joark.
     * I tillegg sjekkes endringssøknaden for påkrevde vedlegg som følger av utsettelse.
     * Alle dokumenter må være mottatt etter vedtaksdatoen på gjeldende innvilgede vedtak.
     *
     * @param behandling
     * @return Liste over manglende vedlegg
     */
    @Override
    public List<ManglendeVedlegg> utledManglendeVedleggForSøknad(BehandlingReferanse ref) {
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
