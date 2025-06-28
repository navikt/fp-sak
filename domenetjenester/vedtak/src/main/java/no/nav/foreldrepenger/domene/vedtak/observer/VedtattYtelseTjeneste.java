package no.nav.foreldrepenger.domene.vedtak.observer;


import java.util.Comparator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.abakus.vedtak.ytelse.Aktør;
import no.nav.abakus.vedtak.ytelse.Kildesystem;
import no.nav.abakus.vedtak.ytelse.Periode;
import no.nav.abakus.vedtak.ytelse.Status;
import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.abakus.vedtak.ytelse.Ytelser;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.Anvisning;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class VedtattYtelseTjeneste {

    private BehandlingVedtakRepository vedtakRepository;
    private BeregningsresultatRepository tilkjentYtelseRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private FamilieHendelseRepository familieHendelseRepository;

    public VedtattYtelseTjeneste() {
        // Plattform trenger tom Ctor (Hibernate, CDI, etc)
    }

    @Inject
    public VedtattYtelseTjeneste(BehandlingVedtakRepository vedtakRepository,
                                 BeregningsresultatRepository tilkjentYtelseRepository,
                                 InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                 FamilieHendelseRepository familieHendelseRepository) {
        this.vedtakRepository = vedtakRepository;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.familieHendelseRepository = familieHendelseRepository;
    }

    public Ytelse genererYtelse(Behandling behandling) {
        var vedtak = vedtakRepository.hentForBehandling(behandling.getId());
        var berResultat = tilkjentYtelseRepository.hentUtbetBeregningsresultat(behandling.getId());

        var aktør = new Aktør();
        aktør.setVerdi(behandling.getAktørId().getId());
        var ytelse = new YtelseV1();
        ytelse.setKildesystem(Kildesystem.FPSAK);
        ytelse.setSaksnummer(behandling.getSaksnummer().getVerdi());
        ytelse.setVedtattTidspunkt(vedtak.getVedtakstidspunkt());
        ytelse.setVedtakReferanse(behandling.getUuid().toString());
        ytelse.setAktør(aktør);
        ytelse.setYtelse(mapYtelser(behandling.getFagsakYtelseType()));
        ytelse.setYtelseStatus(mapStatus(behandling.getFagsak().getStatus()));

        ytelse.setPeriode(utledPeriode(behandling, vedtak, berResultat.orElse(null)));
        ytelse.setAnvist(map(behandling, berResultat.orElse(null)));
        return ytelse;
    }

    private List<Anvisning> map(Behandling behandling, BeregningsresultatEntitet tilkjentYtelse) {
        if (tilkjentYtelse == null) {
            return List.of();
        }
        var arbeidsforholdReferanser = inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId())
                .flatMap(InntektArbeidYtelseGrunnlag::getArbeidsforholdInformasjon)
                .map(ArbeidsforholdInformasjon::getArbeidsforholdReferanser).orElse(List.of());
        return VedtattYtelseMapper.medArbeidsforhold(arbeidsforholdReferanser).mapTilkjent(tilkjentYtelse);
    }


    private Periode utledPeriode(Behandling behandling, BehandlingVedtak vedtak, BeregningsresultatEntitet beregningsresultat) {
        var periode = new Periode();
        if (beregningsresultat != null) {
            var minFom = beregningsresultat.getBeregningsresultatPerioder().stream()
                .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
                .min(Comparator.naturalOrder());
            var maxTom = beregningsresultat.getBeregningsresultatPerioder().stream()
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
        } else if (familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId()).isPresent()) {
            try {
                var stp = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
                    .map(a -> a.getGjeldendeVersjon().getSkjæringstidspunkt())
                    .orElse(vedtak.getVedtaksdato());
                periode.setFom(stp);
                periode.setTom(stp);
                return periode;
            } catch (Exception e) {
                // papirsøknad elns uten fhdato
            }
        }
        periode.setFom(vedtak.getVedtaksdato());
        periode.setTom(vedtak.getVedtaksdato());
        return periode;
    }


    private Ytelser mapYtelser(FagsakYtelseType type) {
        return switch (type) {
            case ENGANGSTØNAD -> Ytelser.ENGANGSTØNAD;
            case FORELDREPENGER -> Ytelser.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> Ytelser.SVANGERSKAPSPENGER;
            default -> throw new IllegalStateException("Ukjent ytelsestype " + type);
        };
    }

    private Status mapStatus(FagsakStatus kode) {
        return switch (kode) {
            case OPPRETTET, UNDER_BEHANDLING -> Status.UNDER_BEHANDLING;
            case LØPENDE -> Status.LØPENDE;
            case AVSLUTTET -> Status.AVSLUTTET;
        };
    }


}
