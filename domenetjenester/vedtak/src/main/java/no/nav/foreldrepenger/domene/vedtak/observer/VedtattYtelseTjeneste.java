package no.nav.foreldrepenger.domene.vedtak.observer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.abakus.iaygrunnlag.kodeverk.Fagsystem;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseStatus;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.vedtak.ytelse.Aktør;
import no.nav.abakus.vedtak.ytelse.Desimaltall;
import no.nav.abakus.vedtak.ytelse.Periode;
import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.Anvisning;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class VedtattYtelseTjeneste {

    private BehandlingVedtakRepository vedtakRepository;
    private BeregningsresultatRepository tilkjentYtelseRepository;

    public VedtattYtelseTjeneste() {
    }

    @Inject
    public VedtattYtelseTjeneste(BehandlingVedtakRepository vedtakRepository, BeregningsresultatRepository tilkjentYtelseRepository) {
        this.vedtakRepository = vedtakRepository;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
    }

    public Ytelse genererYtelse(Behandling behandling) {
        final BehandlingVedtak vedtak = vedtakRepository.hentForBehandling(behandling.getId());
        Optional<BeregningsresultatEntitet> berResultat = tilkjentYtelseRepository.hentBeregningsresultat(behandling.getId());

        final Aktør aktør = new Aktør();
        aktør.setVerdi(behandling.getAktørId().getId());
        final YtelseV1 ytelse = new YtelseV1();
        ytelse.setFagsystem(Fagsystem.FPSAK);
        ytelse.setSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
        ytelse.setVedtattTidspunkt(vedtak.getVedtakstidspunkt());
        ytelse.setVedtakReferanse(behandling.getUuid().toString());
        ytelse.setAktør(aktør);
        ytelse.setType(map(behandling.getFagsakYtelseType()));
        ytelse.setStatus(map(behandling.getFagsak().getStatus()));

        ytelse.setPeriode(utledPeriode(vedtak, berResultat.orElse(null)));
        ytelse.setAnvist(map(berResultat.orElse(null)));
        return ytelse;
    }

    private List<Anvisning> map(BeregningsresultatEntitet uttakResultatEntitet) {
        if (uttakResultatEntitet == null) {
            return List.of();
        }
        return uttakResultatEntitet.getBeregningsresultatPerioder().stream()
            .filter(periode -> periode.getDagsats() > 0)
            .map(this::map)
            .collect(Collectors.toList());
    }

    private Anvisning map(BeregningsresultatPeriode periode) {
        final Anvisning anvisning = new Anvisning();
        final Periode p = new Periode();
        p.setFom(periode.getBeregningsresultatPeriodeFom());
        p.setTom(periode.getBeregningsresultatPeriodeTom());
        anvisning.setPeriode(p);
        anvisning.setDagsats(new Desimaltall(new BigDecimal(periode.getDagsatsFraBg())));
        anvisning.setUtbetalingsgrad(periode.getLavestUtbetalingsgrad().map(Desimaltall::new).orElse(null));
        return anvisning;
    }

    private Periode utledPeriode(BehandlingVedtak vedtak, BeregningsresultatEntitet beregningsresultat) {
        final Periode periode = new Periode();
        if (beregningsresultat != null) {
            Optional<LocalDate> minFom = beregningsresultat.getBeregningsresultatPerioder().stream()
                .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
                .min(Comparator.naturalOrder());
            Optional<LocalDate> maxTom = beregningsresultat.getBeregningsresultatPerioder().stream()
                .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom)
                .max(Comparator.naturalOrder());
            if (minFom.isEmpty()) {
                periode.setFom(vedtak.getVedtaksdato());
                periode.setTom(vedtak.getVedtaksdato());
                return periode;
            }
            periode.setFom(minFom.get());
            if (maxTom.isPresent()) {
                periode.setTom(maxTom.get());
            } else {
                periode.setTom(Tid.TIDENES_ENDE);
            }
            return periode;
        } else {
            periode.setFom(vedtak.getVedtaksdato());
            periode.setTom(vedtak.getVedtaksdato());
        }
        return periode;
    }


    private YtelseType map(FagsakYtelseType type) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(type)) {
            return YtelseType.ENGANGSTØNAD;
        } else if (FagsakYtelseType.FORELDREPENGER.equals(type)) {
            return YtelseType.FORELDREPENGER;
        } else if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(type)) {
            return YtelseType.SVANGERSKAPSPENGER;
        }
        throw new IllegalStateException("Ukjent ytelsestype " + type);
    }

    private YtelseStatus map(FagsakStatus kode) {
        YtelseStatus typeKode;
        if (FagsakStatus.OPPRETTET.equals(kode)) {
            typeKode = YtelseStatus.OPPRETTET;
        } else if (FagsakStatus.UNDER_BEHANDLING.equals(kode)) {
            typeKode = YtelseStatus.UNDER_BEHANDLING;
        } else if (FagsakStatus.LØPENDE.equals(kode)) {
            typeKode = YtelseStatus.LØPENDE;
        } else if (FagsakStatus.AVSLUTTET.equals(kode)) {
            typeKode = YtelseStatus.AVSLUTTET;
        } else {
            typeKode = YtelseStatus.OPPRETTET;
        }
        return typeKode;
    }
}
