package no.nav.foreldrepenger.økonomi.økonomistøtte.kontantytelse.es.adapter;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomi.økonomistøtte.FinnNyesteOppdragForSak;
import no.nav.foreldrepenger.økonomi.økonomistøtte.kontantytelse.es.wrapper.ForrigeOppdragInputES;
import no.nav.foreldrepenger.økonomi.økonomistøtte.kontantytelse.es.wrapper.OppdragInputES;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

@ApplicationScoped
public class MapBehandlingInfoES {

    private static final String KODE_KLASSIFIK_FODSEL = "FPENFOD-OP";
    private static final String KODE_KLASSIFIK_ADOPSJON = "FPENAD-OP";

    private LegacyESBeregningRepository beregningRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private FamilieHendelseRepository familieGrunnlagRepository;
    private FinnNyesteOppdragForSak finnNyesteOppdragForSak;
    private TpsTjeneste tpsTjeneste;

    MapBehandlingInfoES() {
        // for CDI proxy
    }

    @Inject
    public MapBehandlingInfoES(FinnNyesteOppdragForSak finnNyesteOppdragForSak,
                               TpsTjeneste tpsTjeneste,
                               LegacyESBeregningRepository beregningRepository,
                               BehandlingVedtakRepository behandlingVedtakRepository,
                               FamilieHendelseRepository familieHendelseRepository) {
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.beregningRepository = beregningRepository;
        this.familieGrunnlagRepository = familieHendelseRepository;
        this.finnNyesteOppdragForSak = finnNyesteOppdragForSak;
        this.tpsTjeneste = tpsTjeneste;
    }

    public OppdragInputES oppsettBehandlingInfo(Behandling behandling) {
        Saksnummer saksnummer = behandling.getFagsak().getSaksnummer();
        // kallet kan fjernes en gang i fremtiden, når Oppdragssystemet ikke lenger krever fnr i sine meldinger.
        PersonIdent personIdent = tpsTjeneste.hentFnrForAktør(behandling.getAktørId());
        BehandlingVedtak behVedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId()).orElse(null);

        String kodeKlassifik = mapKodeKlassifik(behandling);
        long sats = hentSatsFraBehandling(behandling.getId());
        Optional<ForrigeOppdragInputES> tidligereBehandlingInfo = mapTidligereBehandlinginfo(saksnummer);
        return new OppdragInputES(saksnummer, behandling, behVedtak, personIdent, kodeKlassifik, sats, tidligereBehandlingInfo);
    }

    private Optional<ForrigeOppdragInputES> mapTidligereBehandlinginfo(Saksnummer saksnummer) {

        Optional<Oppdrag110> oppdrag110Opt = finnForrigeOppddragForSak(saksnummer);
        if (oppdrag110Opt.isPresent()) {
            Oppdrag110 tidligereOppdrag110 = oppdrag110Opt.get();
            long tidligereBehandlingId = tidligereOppdrag110.getOppdragskontroll().getBehandlingId();
            BehandlingVedtak tidligereVedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(tidligereBehandlingId)
                .orElseThrow(() -> MapBehandlingInfoESFeil.FACTORY
                    .fantIkkeTidligereBehandlingVedtak(tidligereBehandlingId).toException());
            long sats = hentSatsFraBehandling(tidligereBehandlingId);
            ForrigeOppdragInputES tidligereBehandlingInfo = new ForrigeOppdragInputES(tidligereOppdrag110, tidligereVedtak, sats);
            return Optional.of(tidligereBehandlingInfo);
        }
        return Optional.empty();
    }

    private Optional<Oppdrag110> finnForrigeOppddragForSak(Saksnummer saksnummer) {
        return finnNyesteOppdragForSak.finnNyesteOppdragForSak(saksnummer).stream().findFirst();
    }

    private String mapKodeKlassifik(Behandling behandling) {
        return familieGrunnlagRepository.hentAggregat(behandling.getId()).getGjeldendeVersjon().getGjelderFødsel()
            ? KODE_KLASSIFIK_FODSEL
            : KODE_KLASSIFIK_ADOPSJON;
    }

    private long hentSatsFraBehandling(long behandlingId) {
        Optional<LegacyESBeregning> beregning = beregningRepository.getSisteBeregning(behandlingId);
        return beregning.map(LegacyESBeregning::getBeregnetTilkjentYtelse).orElse(0L);
    }

    interface MapBehandlingInfoESFeil extends DeklarerteFeil {
        MapBehandlingInfoESFeil FACTORY = FeilFactory.create(MapBehandlingInfoESFeil.class);

        @TekniskFeil(feilkode = "FP-131242", feilmelding = "Fant ikke tidligere BehandlingVedtak, behandlingId=%s.", logLevel = LogLevel.ERROR)
        Feil fantIkkeTidligereBehandlingVedtak(long behandlingId);
    }
}
