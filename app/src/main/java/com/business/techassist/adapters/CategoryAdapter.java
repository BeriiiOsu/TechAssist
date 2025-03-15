//package com.business.techassist.adapters;
//
//import android.content.Context;
//import android.content.Intent;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.business.techassist.R;
//import com.business.techassist.category.CategoryModel;
//import com.business.techassist.menucomponents.cart.cart;
//import com.business.techassist.menucomponents.messages.menu_message;
//import com.business.techassist.menucomponents.messages.messagesMenu;
//import com.business.techassist.menucomponents.profileMenu;
//import com.business.techassist.menucomponents.trackOrderMenu;
//
//import java.util.List;
//
//public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
//    Context context;
//    List<CategoryModel> categoryModelList;
//
//    int[] backgrounds = {
//            R.drawable.blue_rec_bg,
//            R.drawable.blue_btn_bg,
//            R.drawable.purple_rec_bg,
//            R.drawable.orange_rec_bg
//    };
//
//    public CategoryAdapter (Context context, List<CategoryModel> categoryModelList){
//        this.context = context;
//        this.categoryModelList = categoryModelList;
//    }
//
//    @NonNull
//    @Override
//    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        return new CategoryViewHolder(LayoutInflater.from(context).inflate(R.layout.category_tabs, parent, false));
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
//        CategoryModel currentPos = categoryModelList.get(position);
//
//        holder.titleCat.setText(currentPos.getCategoryName());
//        holder.picCat.setImageResource(currentPos.getCategoryPicture());
//
//        int backgroundRes = backgrounds[position % backgrounds.length];
//        holder.itemView.setBackgroundResource(backgroundRes);
//
//        holder.itemView.setOnClickListener(view -> {
//            Class<?> targetActivity = getActivityForCategory(currentPos.getCategoryName());
//            if (targetActivity != null) {
//                Intent intent = new Intent(context, targetActivity);
//                intent.putExtra("categoryName", currentPos.getCategoryName());
//                context.startActivity(intent);
//            }
//        });
//    }
//
//    private Class<?> getActivityForCategory(String categoryTitle) {
//        switch (categoryTitle) {
//            case "Profile":
//                return profileMenu.class;
//            case "Cart":
//                return cart.class;
//            case "Track Order":
//                return trackOrderMenu.class;
//            case "Messages":
//                return menu_message.class;
//            default:
//                return null;
//        }
//    }
//
//    @Override
//    public int getItemCount() {
//        return categoryModelList.size();
//    }
//
//    public static class CategoryViewHolder extends RecyclerView.ViewHolder{
//
//        TextView titleCat;
//        ImageView picCat;
//
//        public CategoryViewHolder(@NonNull View itemView) {
//            super(itemView);
//
//            picCat = itemView.findViewById(R.id.picCat);
//            titleCat = itemView.findViewById(R.id.titleCat);
//        }
//    }
//}
