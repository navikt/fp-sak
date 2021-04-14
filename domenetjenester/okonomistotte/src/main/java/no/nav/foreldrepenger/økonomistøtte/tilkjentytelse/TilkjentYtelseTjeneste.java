package no.nav.foreldrepenger.økonomistøtte.tilkjentytelse;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.TilkjentYtelse;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelseBehandlingInfoV1;
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
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var vedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalArgumentException("Vedtak er ikke fattet enda. Denne tjenesten er kun designet for bruk etter at vedtak er fattet."));
        var behandlingsresultat = vedtak.getBehandlingsresultat();

        var tjeneste = FagsakYtelseTypeRef.Lookup.find(tilkjentYtelse, behandling.getFagsakYtelseType()).orElseThrow();

        var perioder = tjeneste.hentTilkjentYtelsePerioder(behandlingId);
        var behandlingsinfo = mapBehandlingsinfo(behandling, vedtak);

        var erOpphør = tjeneste.erOpphør(behandlingsresultat);
        var erOpphørEtterSkjæringstidspunktet = tjeneste.erOpphørEtterSkjæringstidspunkt(behandling, behandlingsresultat);
        var endringsdato = tjeneste.hentEndringstidspunkt(behandlingId);
        return new TilkjentYtelseV1(behandlingsinfo, perioder)
            .setErOpphør(erOpphør)
            .setErOpphørEtterSkjæringstidspunkt(erOpphørEtterSkjæringstidspunktet)
            .setEndringsdato(endringsdato);
    }

    private TilkjentYtelseBehandlingInfoV1 mapBehandlingsinfo(Behandling behandling, BehandlingVedtak vedtak) {
        var gjelderAdopsjon = gjelderAdopsjon(behandling.getId());

        var info = new TilkjentYtelseBehandlingInfoV1();
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
        return familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId)
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .filter(FamilieHendelseEntitet::getGjelderAdopsjon)
            .isPresent();
    }

}
