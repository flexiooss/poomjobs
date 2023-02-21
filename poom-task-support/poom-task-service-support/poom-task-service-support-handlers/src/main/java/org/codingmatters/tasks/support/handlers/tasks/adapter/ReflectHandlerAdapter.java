package org.codingmatters.tasks.support.handlers.tasks.adapter;

import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.value.objects.values.casts.ValueObjectCaster;
import org.codingmatters.value.objects.values.casts.reflect.ValueObjectReflectCaster;

import java.util.function.Function;

public class ReflectHandlerAdapter<Req, Resp, EffectiveReq, EffectiveResp> implements Function<Req, Resp> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(ReflectHandlerAdapter.class);

    private final ValueObjectCaster<Req, EffectiveReq> requestCaster;
    private final ValueObjectCaster<EffectiveResp, Resp> responseCaster;
    private final Function<EffectiveReq, EffectiveResp> deleguate;

    public ReflectHandlerAdapter(Function<EffectiveReq, EffectiveResp> deleguate, Class<Req> reqClass, Class<Resp> respClass, Class<EffectiveReq> effectiveReqClass, Class<EffectiveResp> effectiveRespClass) throws UnadatableHandlerException {
        this.deleguate = deleguate;
        try {
            this.requestCaster = new ValueObjectReflectCaster<>(reqClass, effectiveReqClass);
        } catch (ValueObjectCaster.ValueObjectUncastableException e) {
            throw new UnadatableHandlerException("cannot adapt request", e);
        }
        try {
            this.responseCaster = new ValueObjectReflectCaster<>(effectiveRespClass, respClass);
        } catch (ValueObjectCaster.ValueObjectUncastableException e) {
            throw new UnadatableHandlerException("cannot adapt response", e);
        }
    }

    @Override
    public Resp apply(Req req) {
        EffectiveReq effectiveRequest = null;
        try {
            effectiveRequest = this.requestCaster.cast(req);
        } catch (ValueObjectCaster.ValueObjectCastException e) {
            log.error("[GRAVE] failed converting request", e);
            throw new RuntimeException(e);
        }
        EffectiveResp effectiveResponse = this.deleguate.apply(effectiveRequest);
        Resp response = null;
        try {
            response = this.responseCaster.cast(effectiveResponse);
        } catch (ValueObjectCaster.ValueObjectCastException e) {
            log.error("[GRAVE] failed converting response", e);
            throw new RuntimeException(e);
        }
        return response;
    }
}
