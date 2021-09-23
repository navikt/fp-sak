package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;
import no.nav.foreldrepenger.web.app.util.RestUtils;

public class BehandlingDtoUtil {

    static void settStandardfelterUtvidet(Behandling behandling,
                                          UtvidetBehandlingDto dto,
                                          boolean erBehandlingMedGjeldendeVedtak,
                                          LocalDate vedtaksDato) {
        setStandardfelterMedGjeldendeVedtak(behandling, dto, erBehandlingMedGjeldendeVedtak, vedtaksDato);
              dto.setAnsvarligBeslutter(behandling.getAnsvarligBeslutter());
        dto.setBehandlingHenlagt(behandling.isBehandlingHenlagt());
    }

    private static Optional<String> getFristDatoBehandlingPåVent(Behandling behandling) {
        var frist = behandling.getFristDatoBehandlingPåVent();
        if (frist != null) {
            return Optional.of(frist.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))); //$NON-NLS-1$
        }
        return Optional.empty();
    }

    private static Optional<String> getVenteÅrsak(Behandling behandling) {
        var venteårsak = behandling.getVenteårsak();
        if (venteårsak != null) {
            return Optional.of(venteårsak.getKode());
        }
        return Optional.empty();
    }

    private static List<BehandlingÅrsakDto> lagBehandlingÅrsakDto(Behandling behandling) {
        if (!behandling.getBehandlingÅrsaker().isEmpty()) {
            return behandling.getBehandlingÅrsaker().stream().map(BehandlingDtoUtil::map).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    static void setStandardfelter(Behandling behandling,
                                  BehandlingDto dto,
                                  LocalDate vedtaksDato) {
        dto.setFagsakId(behandling.getFagsakId());
        dto.setId(behandling.getId());
        dto.setUuid(behandling.getUuid());
        dto.setVersjon(behandling.getVersjon());
        dto.setType(behandling.getType());
        dto.setOpprettet(behandling.getOpprettetDato());
        dto.setEndret(behandling.getEndretTidspunkt());
        dto.setEndretAvBrukernavn(behandling.getEndretAv());
        dto.setAvsluttet(behandling.getAvsluttetDato());
        dto.setStatus(behandling.getStatus());
        dto.setBehandlendeEnhetId(behandling.getBehandlendeOrganisasjonsEnhet().enhetId());
        dto.setBehandlendeEnhetNavn(behandling.getBehandlendeOrganisasjonsEnhet().enhetNavn());
        dto.setFørsteÅrsak(førsteÅrsak(behandling).orElse(null));
        dto.setBehandlingsfristTid(behandling.getBehandlingstidFrist());
        dto.setErAktivPapirsøknad(erAktivPapirsøknad(behandling));
        dto.setBehandlingPåVent(behandling.isBehandlingPåVent());
        dto.setBehandlingHenlagt(behandling.isBehandlingHenlagt());
        getFristDatoBehandlingPåVent(behandling).ifPresent(dto::setFristBehandlingPåVent);
        getVenteÅrsak(behandling).ifPresent(dto::setVenteÅrsakKode);
        dto.setOriginalVedtaksDato(vedtaksDato);
        dto.setBehandlingKøet(behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING));
        dto.setAnsvarligSaksbehandler(behandling.getAnsvarligSaksbehandler());
        dto.setToTrinnsBehandling(behandling.isToTrinnsBehandling());
        dto.setBehandlingArsaker(lagBehandlingÅrsakDto(behandling));
    }

    static void setStandardfelterMedGjeldendeVedtak(Behandling behandling,
                                                    BehandlingDto dto,
                                                    boolean erBehandlingMedGjeldendeVedtak,
                                                    LocalDate vedtaksDato) {
        setStandardfelter(behandling, dto, vedtaksDato);
        dto.setGjeldendeVedtak(erBehandlingMedGjeldendeVedtak);
    }

    static Optional<BehandlingÅrsakDto> førsteÅrsak(Behandling behandling) {
        return behandling.getBehandlingÅrsaker().stream()
            .sorted(Comparator.comparing(BaseEntitet::getOpprettetTidspunkt))
            .map(BehandlingDtoUtil::map)
            .findFirst();
    }

    static boolean erAktivPapirsøknad(Behandling behandling) {
        var kriterier = Arrays.asList(
            AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD,
            AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_FORELDREPENGER,
            AksjonspunktDefinisjon.REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER,
            AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_SVANGERSKAPSPENGER);
        return behandling.getÅpneAksjonspunkter(kriterier).size() > 0;
    }

    private static BehandlingÅrsakDto map(BehandlingÅrsak årsak) {
        var dto = new BehandlingÅrsakDto();
        dto.setBehandlingArsakType(årsak.getBehandlingÅrsakType());
        dto.setManueltOpprettet(årsak.erManueltOpprettet());
        return dto;
    }

    static ResourceLink get(String path, String rel, Object dto) {
        return ResourceLink.get(RestUtils.getApiPath(path), rel, dto);
    }

    static ResourceLink post(String path, String rel, Object dto) {
        return ResourceLink.post(RestUtils.getApiPath(path), rel, dto);
    }


}
