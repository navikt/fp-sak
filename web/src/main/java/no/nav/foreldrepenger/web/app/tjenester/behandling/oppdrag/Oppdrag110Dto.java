package no.nav.foreldrepenger.web.app.tjenester.behandling.oppdrag;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;

public class Oppdrag110Dto extends SporingDto {

    private Long fagsystemId;
    private String oppdragGjelderId;
    private String saksbehId;
    private Long oppdragsKontrollId;
    private KodeEndring kodeEndring;
    private KodeFagområde kodeFagomrade;
    private AvstemmingDto avstemming;
    private Ompostering116Dto ompostering116Dto;
    private List<OppdragKvitteringDto> oppdragKvittering;
    private List<Oppdragslinje150Dto> oppdragslinje150;

    public Oppdrag110Dto(Oppdrag110 entitet) {
        super(entitet, 0L, entitet.getId());
    }

    public Long getFagsystemId() {
        return fagsystemId;
    }

    public void setFagsystemId(Long fagsystemId) {
        this.fagsystemId = fagsystemId;
    }

    public String getOppdragGjelderId() {
        return oppdragGjelderId;
    }

    public void setOppdragGjelderId(String oppdragGjelderId) {
        this.oppdragGjelderId = oppdragGjelderId;
    }

    public String getSaksbehId() {
        return saksbehId;
    }

    public void setSaksbehId(String saksbehId) {
        this.saksbehId = saksbehId;
    }

    public Long getOppdragsKontrollId() {
        return oppdragsKontrollId;
    }

    public void setOppdragsKontrollId(Long oppdragsKontrollId) {
        this.oppdragsKontrollId = oppdragsKontrollId;
    }

    public KodeEndring getKodeEndring() {
        return kodeEndring;
    }

    public void setKodeEndring(KodeEndring kodeEndring) {
        this.kodeEndring = kodeEndring;
    }

    public KodeFagområde getKodeFagomrade() {
        return kodeFagomrade;
    }

    public void setKodeFagomrade(KodeFagområde kodeFagomrade) {
        this.kodeFagomrade = kodeFagomrade;
    }

    public AvstemmingDto getAvstemming() {
        return avstemming;
    }

    public void setAvstemming(AvstemmingDto avstemming) {
        this.avstemming = avstemming;
    }

    public List<Oppdragslinje150Dto> getOppdragslinje150() {
        return oppdragslinje150;
    }

    public void setOppdragslinje150(List<Oppdragslinje150Dto> oppdragslinje150) {
        this.oppdragslinje150 = oppdragslinje150;
    }

    public static Oppdrag110Dto fraDomene(Oppdrag110 oppdrag110) {
        var oppdrag110Dto = new Oppdrag110Dto(oppdrag110);
        oppdrag110Dto.fagsystemId = oppdrag110.getFagsystemId();
        oppdrag110Dto.oppdragGjelderId = oppdrag110.getOppdragGjelderId();
        oppdrag110Dto.saksbehId = oppdrag110.getSaksbehId();
        oppdrag110Dto.oppdragsKontrollId = oppdrag110.getOppdragskontroll().getId();
        oppdrag110Dto.kodeEndring = oppdrag110.getKodeEndring();
        oppdrag110Dto.kodeFagomrade = oppdrag110.getKodeFagomrade();
        oppdrag110Dto.avstemming = AvstemmingDto.fraDomene(oppdrag110.getAvstemming());

        oppdrag110Dto.oppdragKvittering = oppdrag110.erKvitteringMottatt() ? List.of(
            OppdragKvitteringDto.fraDomene(oppdrag110.getOppdragKvittering())) : List.of();
        var optOmposter = oppdrag110.getOmpostering116();
        oppdrag110Dto.oppdragslinje150 = oppdrag110.getOppdragslinje150Liste().stream().map(Oppdragslinje150Dto::fraDomene).toList();

        if (optOmposter.isPresent()) {
            oppdrag110Dto.ompostering116Dto = Ompostering116Dto.fraDomene(optOmposter.get());
        }
        oppdrag110Dto.oppdragsKontrollId = oppdrag110.getOppdragskontroll().getId();
        return oppdrag110Dto;
    }

    public Ompostering116Dto getOmpostering116Dto() {
        return ompostering116Dto;
    }

    public Oppdrag110Dto setOmpostering116Dto(Ompostering116Dto ompostering116Dto) {
        this.ompostering116Dto = ompostering116Dto;
        return this;
    }

    public List<OppdragKvitteringDto> getOppdragKvittering() {
        return oppdragKvittering;
    }

}
