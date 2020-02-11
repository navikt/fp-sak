package no.nav.foreldrepenger.domene.vedtak;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingOverlappInfotrygd;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingOverlappInfotrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.InfotrygdHendelse;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.Meldingstype;

@ApplicationScoped
public class IdentifiserOverlappendeInfotrygdYtelseTjeneste {

    private static final Logger log = LoggerFactory.getLogger(IdentifiserOverlappendeInfotrygdYtelseTjeneste.class);
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;
    private BehandlingOverlappInfotrygdRepository overlappRepository;

    IdentifiserOverlappendeInfotrygdYtelseTjeneste() {
        // for CDI
    }

    @Inject
    public IdentifiserOverlappendeInfotrygdYtelseTjeneste(BeregningsresultatRepository beregningsresultatRepository,
                                                          InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                          BehandlingOverlappInfotrygdRepository overlappRepository) {
        this.iayTjeneste = inntektArbeidYtelseTjeneste;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.overlappRepository = overlappRepository;
    }

    public void vurderOgLagreEventueltOverlapp(Behandling behandling, BehandlingVedtak behandlingVedtak) {
        Optional<BehandlingOverlappInfotrygd> overlapp = this.vurder(behandling, behandlingVedtak);
        overlapp.ifPresent(behandlingOverlappInfotrygd -> overlappRepository.lagre(behandlingOverlappInfotrygd));
    }

    public Optional<BehandlingOverlappInfotrygd> vurder(Behandling behandling, BehandlingVedtak behandlingVedtak) {
        Long behandlingId = behandling.getId();
        Optional<ÅpenDatoIntervallEntitet> periodeVLOpt = fastsettPeriodeVL(behandlingId);
        if (!VedtakResultatType.INNVILGET.equals(behandlingVedtak.getVedtakResultatType())
            || periodeVLOpt.isEmpty()) {
            return Optional.empty();
        }
        ÅpenDatoIntervallEntitet periodeVL = periodeVLOpt.get();
        Collection<Ytelse> ytelseList = hentRegisterDataFraInfotrygd(behandlingId, behandling.getAktørId(), periodeVL);

        // Denne ble forsøkt hentet fra infotrygd men har vært tom i mange måneder. Dermed er overlapp basert på Abakus
        // Her bør vi se bort fra abakus og bare lagre overlapp mot SYK og OMS.
        // Mesteparten av koden under kan slettes og betydelig forenkles
        List<InfotrygdHendelse> hendelseListe = Collections.emptyList();
        boolean finnesMatchMedInnvilIRegisterData = sjekkMotRegisterData(hendelseListe, ytelseList);
        if (finnesMatchMedInnvilIRegisterData) {
            log.info("Det var en hendelse av type ANNULERT fra infotrygd feed som korrelerer med en INNVILGET ytelse i registerdata");
        }
        Optional<InfotrygdHendelse> nyesteHendelse = finnNyesteHendelse(hendelseListe);

        return vurderHendelse(behandling, nyesteHendelse, ytelseList, periodeVL);
    }

    private Optional<ÅpenDatoIntervallEntitet> fastsettPeriodeVL(Long behandlingId) {
        var beregningsresultatOpt = beregningsresultatRepository.hentUtbetBeregningsresultat(behandlingId);
        var beregningsresultatPerioder = beregningsresultatOpt.map(BeregningsresultatEntitet::getBeregningsresultatPerioder)
            .orElse(Collections.emptyList());
        List<BeregningsresultatPeriode> perioderMedInnvilgetYtelse = beregningsresultatPerioder.stream()
            .filter(brPeriode -> brPeriode.getBeregningsresultatAndelList().stream()
                .anyMatch(andel -> andel.getDagsats() > 0))
            .collect(Collectors.toList());

        if (perioderMedInnvilgetYtelse.isEmpty()) {
            return Optional.empty();
        }

        LocalDate førsteUttaksdato = perioderMedInnvilgetYtelse.stream()
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
            .min(Comparator.naturalOrder())
            .get();
        LocalDate sisteUttaksdato = perioderMedInnvilgetYtelse.stream()
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom)
            .max(Comparator.naturalOrder())
            .get();
        ÅpenDatoIntervallEntitet vlPeriode = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(førsteUttaksdato, sisteUttaksdato);
        return Optional.of(vlPeriode);
    }

    private Collection<Ytelse> hentRegisterDataFraInfotrygd(Long behandlingId, AktørId aktørId, ÅpenDatoIntervallEntitet periodeVL) {
        var ytelseFilter = iayTjeneste.finnGrunnlag(behandlingId)
            .map(it -> new YtelseFilter(it.getAktørYtelseFraRegister(aktørId))).orElse(YtelseFilter.EMPTY);

        return ytelseFilter
            .filter(ytelse -> ytelse.getKilde().equals(Fagsystem.INFOTRYGD))
            .filter(ytelse -> periodeVL.overlapper(ytelse.getPeriode()))
            .filter(ytelse -> Arrays.asList(RelatertYtelseTilstand.LØPENDE, RelatertYtelseTilstand.AVSLUTTET).contains(ytelse.getStatus()))
            .getFiltrertYtelser();
    }

    private boolean sjekkMotRegisterData(List<InfotrygdHendelse> hendelseListe, Collection<Ytelse> ytelseList) {
        Map<String, List<InfotrygdHendelse>> groupedByType = hendelseListe.stream()
            .collect(Collectors.groupingBy(InfotrygdHendelse::getType));
        List<InfotrygdHendelse> listAnnulSammenMedOpphInnv = fjernAnnulertSomErKorrelertMedInnvEllerOpph(groupedByType);
        List<InfotrygdHendelse> listMedKunAnnul = listAnnulSammenMedOpphInnv.stream()
            .filter(infotrygdHendelse -> infotrygdHendelse.getType().equals(Meldingstype.INFOTRYGD_ANNULLERT.getType())).collect(Collectors.toList());

        for (InfotrygdHendelse hendelse : listMedKunAnnul) {
            LocalDate identDato = konverterTilLocalDate(hendelse.getIdentDato());
            boolean finnesInnvilMatchIRegData = ytelseList.stream().anyMatch(ytelse -> ytelse.getPeriode().getFomDato().equals(identDato));
            if (finnesInnvilMatchIRegData) {
                return true;
            }
        }
        return false;
    }

    private Optional<InfotrygdHendelse> finnNyesteHendelse(List<InfotrygdHendelse> hendelseListe) {

        Map<String, List<InfotrygdHendelse>> groupedByType = hendelseListe.stream()
            .collect(Collectors.groupingBy(InfotrygdHendelse::getType));
        List<InfotrygdHendelse> filtrertList = fjernAnnulertSomErKorrelertMedInnvEllerOpph(groupedByType);
        filtrertList.removeIf(infotrygdHendelse -> infotrygdHendelse.getType().equals(Meldingstype.INFOTRYGD_ANNULLERT.getType()));
        filtrertList.removeIf(infotrygdHendelse -> infotrygdHendelse.getType().equals(Meldingstype.INFOTRYGD_ENDRET.getType()));

        return filtrertList.stream().max(Comparator.comparing(InfotrygdHendelse::getSekvensnummer));
    }

    private List<InfotrygdHendelse> fjernAnnulertSomErKorrelertMedInnvEllerOpph(Map<String, List<InfotrygdHendelse>> groupedByType) {
        String keyANNUL = Meldingstype.INFOTRYGD_ANNULLERT.getType();
        List<InfotrygdHendelse> toRemove = new ArrayList<>();
        List<InfotrygdHendelse> alleHendelser = groupedByType.values().stream().flatMap(Collection::stream).collect(Collectors.toList());

        if (!groupedByType.isEmpty() && groupedByType.containsKey(keyANNUL)) {
            for (InfotrygdHendelse hendelse : groupedByType.get(keyANNUL)) {
                boolean matchMedOpphørt = fjernAnnulertOgOpphørtSomMatches(groupedByType, hendelse, toRemove);
                if (!matchMedOpphørt) {
                    fjernAnnulertOgInnvilgetSomMatches(groupedByType, hendelse, toRemove);
                }
            }
            for (InfotrygdHendelse ih : toRemove) {
                alleHendelser.remove(ih);
            }
        }
        return alleHendelser;
    }

    private boolean fjernAnnulertOgOpphørtSomMatches(Map<String, List<InfotrygdHendelse>> groupedByType, InfotrygdHendelse hendelse,
                                                     List<InfotrygdHendelse> toRemove) {
        String keyOPPH = Meldingstype.INFOTRYGD_OPPHOERT.getType();
        Optional<InfotrygdHendelse> opphOpt = groupedByType.containsKey(keyOPPH) ? groupedByType.get(keyOPPH)
            .stream()
            .filter(ih -> ih.getIdentDato().equals(hendelse.getIdentDato()) && ih.getTypeYtelse().equals(hendelse.getTypeYtelse()))
            .findFirst() : Optional.empty();

        if (opphOpt.isPresent()) {
            toRemove.add(opphOpt.get());
            return toRemove.add(hendelse);
        }
        return false;
    }

    private void fjernAnnulertOgInnvilgetSomMatches(Map<String, List<InfotrygdHendelse>> groupedByType, InfotrygdHendelse hendelse,
                                                    List<InfotrygdHendelse> toRemove) {
        String keyINNVIL = Meldingstype.INFOTRYGD_INNVILGET.getType();
        Optional<InfotrygdHendelse> innvilOpt = groupedByType.containsKey(keyINNVIL) ? groupedByType.get(keyINNVIL)
            .stream()
            .filter(ih -> ih.getIdentDato().equals(hendelse.getIdentDato()) && ih.getTypeYtelse().equals(hendelse.getTypeYtelse()))
            .findFirst() : Optional.empty();

        if (innvilOpt.isPresent()) {
            toRemove.add(innvilOpt.get());
            toRemove.add(hendelse);
        }
    }

    private LocalDate konverterTilLocalDate(String identDatoStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        return LocalDate.parse(identDatoStr, formatter);
    }

    private Optional<BehandlingOverlappInfotrygd> vurderHendelse(Behandling behandling, Optional<InfotrygdHendelse> nyesteHendelsen, Collection<Ytelse> ytelseList, ÅpenDatoIntervallEntitet periodeVL) {
        Optional<InfotrygdHendelse> innvilgetHendelse = nyesteHendelsen
            .filter(hendelse -> hendelse.getType().equals(Meldingstype.INFOTRYGD_INNVILGET.getType()));
        if (innvilgetHendelse.isPresent()) {
            InfotrygdHendelse infotrygdHendelse = innvilgetHendelse.get();
            return lagOverlappInfotrygd(behandling, ytelseList, periodeVL, infotrygdHendelse);
        }
        Optional<InfotrygdHendelse> opphørHendelseOpt = nyesteHendelsen
            .filter(hendelse -> hendelse.getType().equals(Meldingstype.INFOTRYGD_OPPHOERT.getType()));
        if (opphørHendelseOpt.isPresent()) { // NOSONAR
            InfotrygdHendelse opphørHendelse = opphørHendelseOpt.get();
            return håndterOpphørshendelse(behandling, ytelseList, periodeVL, opphørHendelse);
        }
        return håndterIngenHendelse(behandling, ytelseList, periodeVL);
    }

    private Optional<BehandlingOverlappInfotrygd> håndterOpphørshendelse(Behandling behandling, Collection<Ytelse> ytelseList, ÅpenDatoIntervallEntitet periodeVL, InfotrygdHendelse opphørHendelse) {
        LocalDate opphørFom = opphørHendelse.getFom();
        if (!opphørFom.isAfter(periodeVL.getFomDato())) {
            return Optional.empty();
        } else {
            return lagOverlappInfotrygd(behandling, ytelseList, periodeVL, opphørHendelse);
        }
    }

    private Optional<BehandlingOverlappInfotrygd> håndterIngenHendelse(Behandling behandling, Collection<Ytelse> ytelseList, ÅpenDatoIntervallEntitet periodeVL) {
        if (ytelseList.isEmpty()) {
            return Optional.empty();
        } else {
            return lagBehandlingOverlappInfotrygd(behandling, ytelseList.iterator().next(), periodeVL);
        }
    }

    private Optional<BehandlingOverlappInfotrygd> lagOverlappInfotrygd(Behandling behandling, Collection<Ytelse> ytelseList, ÅpenDatoIntervallEntitet periodeVL, InfotrygdHendelse infotrygdHendelse) {
        if (ytelseList.isEmpty()) {
            return lagBehandlingOverlappInfotrygd(behandling, infotrygdHendelse, periodeVL);
        } else {
            return lagBehandlingOverlappInfotrygd(behandling, ytelseList.iterator().next(), periodeVL);
        }
    }

    private Optional<BehandlingOverlappInfotrygd> lagBehandlingOverlappInfotrygd(Behandling behandling, Ytelse ytelse, ÅpenDatoIntervallEntitet periodeVL) {
        ÅpenDatoIntervallEntitet periodeInfotrygd = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(ytelse.getPeriode().getFomDato(), ytelse.getPeriode().getTomDato());
        BehandlingOverlappInfotrygd behandlingOverlappInfotrygd = BehandlingOverlappInfotrygd.builder()
            .medSaksnummer(behandling.getFagsak().getSaksnummer())
            .medBehandlingId(behandling.getId())
            .medPeriodeInfotrygd(periodeInfotrygd)
            .medPeriodeVL(periodeVL)
            .build();
        return Optional.of(behandlingOverlappInfotrygd);
    }

    private Optional<BehandlingOverlappInfotrygd> lagBehandlingOverlappInfotrygd(Behandling behandling, InfotrygdHendelse infotrygdHendelse, ÅpenDatoIntervallEntitet periodeVL) {
        ÅpenDatoIntervallEntitet periodeInfotrygd = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(infotrygdHendelse.getFom(), null);
        BehandlingOverlappInfotrygd behandlingOverlappInfotrygd = BehandlingOverlappInfotrygd.builder()
            .medSaksnummer(behandling.getFagsak().getSaksnummer())
            .medBehandlingId(behandling.getId())
            .medPeriodeInfotrygd(periodeInfotrygd)
            .medPeriodeVL(periodeVL)
            .build();
        return Optional.of(behandlingOverlappInfotrygd);
    }

}
