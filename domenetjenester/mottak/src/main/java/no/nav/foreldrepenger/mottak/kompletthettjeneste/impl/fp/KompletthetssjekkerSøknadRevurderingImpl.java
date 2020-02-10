package no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.fp;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
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
import no.nav.foreldrepenger.behandlingslager.kodeverk.arkiv.DokumentType;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@BehandlingTypeRef("BT-004")
@FagsakYtelseTypeRef("FP")
public class KompletthetssjekkerSøknadRevurderingImpl extends KompletthetssjekkerSøknadImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(KompletthetssjekkerSøknadRevurderingImpl.class);

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
        Long behandlingId = ref.getBehandlingId();

        final Optional<SøknadEntitet> søknad = søknadRepository.hentSøknadHvisEksisterer(behandlingId);

        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        LocalDate vedtaksdato = behandlingVedtakRepository.hentBehandlingVedtakFraRevurderingensOriginaleBehandling(behandling).getVedtaksdato();
        List<DokumentType> mottatteDokumentTypeIder = mottatteDokumentRepository.hentMottatteDokumentVedleggPåBehandlingId(behandlingId)
            .stream().map(MottattDokument::getDokumentType).collect(toList());
        LocalDate sammenligningsdato = søknad.map(SøknadEntitet::getSøknadsdato).map(vedtaksdato::isAfter).orElse(Boolean.FALSE) ? søknad.get().getSøknadsdato()
            : vedtaksdato;

        Set<DokumentType> arkivDokumentTypeIds = dokumentArkivTjeneste.hentDokumentTypeIdForSak(ref.getSaksnummer(), sammenligningsdato,
            mottatteDokumentTypeIder);

        final List<ManglendeVedlegg> manglendeVedlegg = identifiserManglendeVedlegg(søknad, arkivDokumentTypeIds);
        Optional<OppgittFordelingEntitet> oppgittFordeling = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId)
            .map(YtelseFordelingAggregat::getOppgittFordeling);
        final List<ManglendeVedlegg> manglendeVedleggUtsettelse = identifiserManglendeVedleggSomFølgerAvUtsettelse(oppgittFordeling, arkivDokumentTypeIds);
        manglendeVedlegg.addAll(manglendeVedleggUtsettelse);

        if (!manglendeVedlegg.isEmpty()) {
            LOGGER.info("Behandling {} er ikke komplett - mangler følgende vedlegg til søknad: {}", behandlingId,
                lagDokumentTypeString(manglendeVedlegg)); // NOSONAR //$NON-NLS-1$
        }
        return manglendeVedlegg;
    }

    private List<ManglendeVedlegg> identifiserManglendeVedleggSomFølgerAvUtsettelse(Optional<OppgittFordelingEntitet> oppgittFordeling,
                                                                                    Set<DokumentType> dokumentTypeIdSet) {
        if (!oppgittFordeling.isPresent()) {
            return emptyList();
        }

        List<ManglendeVedlegg> manglendeVedlegg = new ArrayList<>();
        List<OppgittPeriodeEntitet> oppgittePerioder = oppgittFordeling.get().getOppgittePerioder();

        oppgittePerioder.stream().map(OppgittPeriodeEntitet::getÅrsak).forEach(årsak -> {
            if (UtsettelseÅrsak.SYKDOM.equals(årsak) && !dokumentTypeIdSet.contains(DokumentTypeId.LEGEERKLÆRING)) {
                manglendeVedlegg.add(new ManglendeVedlegg(DokumentTypeId.LEGEERKLÆRING));
            } else if ((UtsettelseÅrsak.INSTITUSJON_SØKER.equals(årsak) || UtsettelseÅrsak.INSTITUSJON_BARN.equals(årsak))
                && !dokumentTypeIdSet.contains(DokumentTypeId.DOK_INNLEGGELSE)) {
                manglendeVedlegg.add(new ManglendeVedlegg(DokumentTypeId.DOK_INNLEGGELSE));
            }
        });

        return manglendeVedlegg;
    }

    private String lagDokumentTypeString(List<ManglendeVedlegg> manglendeVedlegg) {
        return manglendeVedlegg.stream().map(mv -> mv.getDokumentType().getKode()).collect(toList()).toString();
    }
}
