package no.nav.foreldrepenger.dokumentbestiller.brevmal;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingStatus;
import no.nav.foreldrepenger.kontrakter.formidling.v1.BrevmalDto;

@Dependent
public class BrevmalTjeneste {
    private DokumentBehandlingTjeneste dokumentTjeneste;
    private ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste;

    BrevmalTjeneste() {
        // for cdi proxy
    }

    @Inject
    public BrevmalTjeneste(DokumentBehandlingTjeneste dokumentTjeneste,
                           ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste) {
        this.dokumentTjeneste = dokumentTjeneste;
        this.arbeidsforholdInntektsmeldingMangelTjeneste = arbeidsforholdInntektsmeldingMangelTjeneste;
    }

    public List<BrevmalDto> hentBrevmalerFor(Behandling behandling) {
        var kandidater = new HashSet<>(DokumentMalType.MANUELLE_BREV);
        var ikkeRelevant = finnIrrelevanteMaler(behandling.getFagsakYtelseType(), behandling.getType(), behandling.erManueltOpprettet());
        kandidater.removeAll(ikkeRelevant);
        return mapTilDto(behandling, kandidater);
    }

    // Fjerner dokumentmaler som ikke er tilgjengelig for manuell utsendelse, og for ulike behandlingstyper
    private Set<DokumentMalType> finnIrrelevanteMaler(FagsakYtelseType ytelseType,
                                                      BehandlingType behandlingType,
                                                      boolean erBehandlingManueltOpprettet) {
        Set<DokumentMalType> fjernes = new HashSet<>();

        if (BehandlingType.REVURDERING.equals(behandlingType)) {
            if (erBehandlingManueltOpprettet) {
                fjernes.add(DokumentMalType.FORLENGET_SAKSBEHANDLINGSTID);
                fjernes.addAll(DokumentMalType.forlengetSaksbehandlingstidMedlemskap());
            }
        } else if (BehandlingType.KLAGE.equals(behandlingType) || BehandlingType.INNSYN.equals(behandlingType)) {
            fjernes.add(DokumentMalType.ETTERLYS_INNTEKTSMELDING);
            fjernes.addAll(DokumentMalType.forlengetSaksbehandlingstidMedlemskap());
            fjernes.add(DokumentMalType.VARSEL_OM_REVURDERING);
        } else {
            fjernes.add(DokumentMalType.VARSEL_OM_REVURDERING);
        }

        fjernes.addAll(finnIrrelevanteMalerPåYtelseType(ytelseType, behandlingType, erBehandlingManueltOpprettet));
        return fjernes;
    }

    private Set<DokumentMalType> finnIrrelevanteMalerPåYtelseType(FagsakYtelseType ytelseType,
                                                                  BehandlingType behandlingType,
                                                                  boolean erBehandlingManueltOpprettet) {
        return switch (ytelseType) {
            case ENGANGSTØNAD -> finnIrrelevanteMalerES(behandlingType, erBehandlingManueltOpprettet);
            case FORELDREPENGER, SVANGERSKAPSPENGER -> DokumentMalType.forlengetSaksbehandlingstidMedlemskap();
            case UDEFINERT -> throw new IllegalArgumentException("Støtter ikke " + behandlingType);
        };
    }

    private Set<DokumentMalType> finnIrrelevanteMalerES(BehandlingType behandlingType, boolean erBehandlingManueltOpprettet) {
        // Engangsstønad revurdering har en egen VARSEL_OM_REVURDERING aksjonspunkt - så dette brevet må fjernes fra panelet.
        // Inntektsmelding trenges ikke heler.
        var fjernes = new HashSet<DokumentMalType>();
        fjernes.add(DokumentMalType.ETTERLYS_INNTEKTSMELDING);
        if (BehandlingType.REVURDERING.equals(behandlingType)) {
            fjernes.add(DokumentMalType.FORLENGET_SAKSBEHANDLINGSTID);
            fjernes.addAll(DokumentMalType.forlengetSaksbehandlingstidMedlemskap());
            if (erBehandlingManueltOpprettet) {
                fjernes.add(DokumentMalType.VARSEL_OM_REVURDERING);
            }
        }
        return fjernes;
    }

    /*
     * Markerer som ikke tilgjengelige de brevmaler som ikke er aktuelle i behandlingen
     */
    private List<BrevmalDto> mapTilDto(Behandling behandling, Set<DokumentMalType> dokumentMaler) {
        return dokumentMaler.stream()
            .map(dokumentMal -> new BrevmalDto(dokumentMal.getKode(), dokumentMal.getNavn(),
                sjekkOmDokumentSkalVæreTilgjengeligForSaksbehandler(behandling, dokumentMal)))
            .sorted(Comparator.comparing(BrevmalDto::tilgjengelig).thenComparing(BrevmalDto::kode).reversed())
            .toList();
    }

    boolean sjekkOmDokumentSkalVæreTilgjengeligForSaksbehandler(Behandling behandling, DokumentMalType dokumentMal) {
        if (DokumentMalType.forlengetSaksbehandlingstidMedlemskap().contains(dokumentMal)) {
            return erÅpenBehandlingOgDokumentIkkeBestilt(behandling, dokumentMal);
        }
        return switch (dokumentMal) {
            case VARSEL_OM_REVURDERING -> erÅpenBehandlingOgDokumentIkkeBestilt(behandling, dokumentMal);
            case FORLENGET_SAKSBEHANDLINGSTID -> erÅpenBehandling(behandling);
            case ETTERLYS_INNTEKTSMELDING -> erÅpenBehandling(behandling) && manglerInntektsmelding(behandling);
            default -> true;
        };
    }

    private boolean manglerInntektsmelding(Behandling behandling) {
        var arbeidsforhold = arbeidsforholdInntektsmeldingMangelTjeneste.finnStatusForInntektsmeldingArbeidsforhold(
            BehandlingReferanse.fra(behandling));
        return arbeidsforhold.stream()
            .anyMatch(af -> ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT.equals(af.inntektsmeldingStatus()));
    }

    private boolean erÅpenBehandling(Behandling behandling) {
        return !behandling.erSaksbehandlingAvsluttet();
    }

    private boolean erÅpenBehandlingOgDokumentIkkeBestilt(Behandling behandling, DokumentMalType dokumentMal) {
        return erÅpenBehandling(behandling) && erIkkeDokumentBestilt(behandling.getId(), dokumentMal);
    }

    private boolean erIkkeDokumentBestilt(long behandlingId, DokumentMalType dokumentMal) {
        return !dokumentTjeneste.erDokumentBestilt(behandlingId, dokumentMal);
    }

}
