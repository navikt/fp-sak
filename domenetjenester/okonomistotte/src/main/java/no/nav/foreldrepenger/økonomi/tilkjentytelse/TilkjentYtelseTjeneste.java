package no.nav.foreldrepenger.økonomi.tilkjentytelse;

import java.time.LocalDate;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.TilkjentYtelse;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelseBehandlingInfoV1;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelsePeriodeV1;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelseV1;

@ApplicationScoped
public class TilkjentYtelseTjeneste {

    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private Instance<YtelseTypeTilkjentYtelseTjeneste> tilkjentYtelse;

    TilkjentYtelseTjeneste() {
        //for CDI proxy
    }

    @Inject
    public TilkjentYtelseTjeneste(BehandlingRepository behandlingRepository,
                                  BehandlingVedtakRepository behandlingVedtakRepository,
                                  FamilieHendelseRepository familieHendelseRepository,
                                  @Any Instance<YtelseTypeTilkjentYtelseTjeneste> tilkjentYtelse) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.tilkjentYtelse = tilkjentYtelse;
    }

    public TilkjentYtelse hentilkjentYtelse(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        BehandlingVedtak vedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalArgumentException("Vedtak er ikke fattet enda. Denne tjenesten er kun designet for bruk etter at vedtak er fattet."));
        Behandlingsresultat behandlingsresultat = vedtak.getBehandlingsresultat();

        YtelseTypeTilkjentYtelseTjeneste tjeneste = FagsakYtelseTypeRef.Lookup.find(tilkjentYtelse, behandling.getFagsakYtelseType()).orElseThrow();

        List<TilkjentYtelsePeriodeV1> perioder = tjeneste.hentTilkjentYtelsePerioder(behandlingId);
        TilkjentYtelseBehandlingInfoV1 behandlingsinfo = mapBehandlingsinfo(behandling, vedtak);

        boolean erOpphør = tjeneste.erOpphør(behandlingsresultat);
        Boolean erOpphørEtterSkjæringstidspunktet = tjeneste.erOpphørEtterSkjæringstidspunkt(behandling, behandlingsresultat);
        LocalDate endringsdato = tjeneste.hentEndringstidspunkt(behandlingId);
        return new TilkjentYtelseV1(behandlingsinfo, perioder)
            .setErOpphør(erOpphør)
            .setErOpphørEtterSkjæringstidspunkt(erOpphørEtterSkjæringstidspunktet)
            .setEndringsdato(endringsdato);
    }

    private TilkjentYtelseBehandlingInfoV1 mapBehandlingsinfo(Behandling behandling, BehandlingVedtak vedtak) {
        boolean gjelderAdopsjon = gjelderAdopsjon(behandling.getId());

        TilkjentYtelseBehandlingInfoV1 info = new TilkjentYtelseBehandlingInfoV1();
        info.setSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
        info.setBehandlingId(behandling.getId());
        info.setYtelseType(MapperForYtelseType.mapYtelseType(behandling.getFagsakYtelseType()));
        info.setAnsvarligSaksbehandler(vedtak.getAnsvarligSaksbehandler());
        info.setGjelderAdopsjon(gjelderAdopsjon);
        info.setAktørId(behandling.getAktørId().getId());
        info.setVedtaksdato(vedtak.getVedtaksdato());
        behandling.getOriginalBehandlingId().ifPresent(info::setForrigeBehandlingId);
        return info;
    }

    private boolean gjelderAdopsjon(Long behandlingId) {
        return familieHendelseRepository.hentAggregat(behandlingId)
            .getGjeldendeVersjon()
            .getGjelderAdopsjon();
    }

}
